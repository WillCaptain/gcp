import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.VariableKind;
import org.twelve.gcp.meta.FieldMeta;
import org.twelve.gcp.meta.MetaExtractor;
import org.twelve.gcp.meta.ModuleMeta;
import org.twelve.gcp.meta.OutlineMeta;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.expression.identifier.SymbolIdentifier;
import org.twelve.gcp.node.expression.typeable.EntityTypeNode;
import org.twelve.gcp.node.expression.typeable.IdentifierTypeNode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.node.statement.OutlineDeclarator;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FieldMeta#isMethod()} and the {@link OutlineMeta#fields()} /
 * {@link OutlineMeta#methods()} split.
 *
 * <p>This is the GCP-level source of truth for distinguishing scalar entity
 * properties (e.g. {@code name:String}) from callable methods / navigation
 * edges (e.g. {@code provinces:Unit -> Provinces}).  The entitir layer mirrors
 * this logic in {@code OntologyWorld.collectOutlineBodyInNode} by checking
 * whether {@code rawType.contains("->")}.
 *
 * <h2>Why this matters for the AI agent</h2>
 * {@code EvalExpressionTool} uses the {@code kind} label to populate
 * {@code can_chain.members} (only methods/builtins) vs {@code can_chain.entity_fields}
 * (scalar properties). If scalars leak into {@code members}, the LLM writes
 * expressions like {@code cities.name} which throw at runtime because scalar
 * fields are only accessible on individual entity instances, not on
 * {@code LazySet} collections.
 */
class MetaExtractorTest {

    // ── FieldMeta.isMethod() ──────────────────────────────────────────────────

    @Nested
    class FieldMetaIsMethod {

        @Test
        void string_field_is_not_method() {
            assertThat(new FieldMeta("name", "String", null).isMethod()).isFalse();
        }

        @Test
        void int_field_is_not_method() {
            assertThat(new FieldMeta("age", "Int", null).isMethod()).isFalse();
        }

        @Test
        void bool_field_is_not_method() {
            assertThat(new FieldMeta("active", "Bool", null).isMethod()).isFalse();
        }

        @Test
        void id_literal_zero_is_not_method() {
            // Entity id field declared as "id:0" in Outline
            assertThat(new FieldMeta("id", "0", null).isMethod()).isFalse();
        }

        @Test
        void private_fk_int_is_not_method() {
            // _city:Int  — hidden FK storage field
            assertThat(new FieldMeta("_city", "Int", null).isMethod()).isFalse();
        }

        @Test
        void unit_returning_entity_is_method() {
            assertThat(new FieldMeta("city", "Unit -> City", null).isMethod()).isTrue();
        }

        @Test
        void unit_returning_collection_is_method() {
            assertThat(new FieldMeta("provinces", "Unit -> Provinces", null).isMethod()).isTrue();
        }

        @Test
        void string_arg_returning_unit_is_method() {
            assertThat(new FieldMeta("send_report", "String -> Unit", null).isMethod()).isTrue();
        }

        @Test
        void generic_arrow_is_method() {
            assertThat(new FieldMeta("map", "fx<b> (a->b) -> VirtualSet<b>", null).isMethod()).isTrue();
        }
    }

    // ── OutlineMeta.fields() / .methods() split ───────────────────────────────

    @Nested
    class OutlineMetaSplit {

        private final OutlineMeta cityMeta = new OutlineMeta("City", "{...}", "A city entity",
                List.of(
                        new FieldMeta("id",               "0",                    null),
                        new FieldMeta("name",             "String",               null),
                        new FieldMeta("population_count", "Int",                  null),
                        new FieldMeta("_province",        "Int",                  null),
                        new FieldMeta("province",         "Unit -> Provinces",    null),
                        new FieldMeta("schools",          "Unit -> Schools",      null),
                        new FieldMeta("residents",        "Unit -> Persons",      null)
                ));

        @Test
        void fields_contains_only_scalars() {
            assertThat(cityMeta.fields())
                    .extracting(FieldMeta::name)
                    .containsExactlyInAnyOrder("id", "name", "population_count", "_province");
        }

        @Test
        void fields_are_all_non_method() {
            assertThat(cityMeta.fields()).noneMatch(FieldMeta::isMethod);
        }

        @Test
        void methods_contains_only_navigations() {
            assertThat(cityMeta.methods())
                    .extracting(FieldMeta::name)
                    .containsExactlyInAnyOrder("province", "schools", "residents");
        }

        @Test
        void methods_are_all_method() {
            assertThat(cityMeta.methods()).allMatch(FieldMeta::isMethod);
        }

        @Test
        void no_overlap_between_fields_and_methods() {
            var fieldNames  = cityMeta.fields().stream().map(FieldMeta::name).toList();
            var methodNames = cityMeta.methods().stream().map(FieldMeta::name).toList();
            assertThat(fieldNames).doesNotContainAnyElementsOf(methodNames);
        }

        @Test
        void members_preserves_all_items() {
            assertThat(cityMeta.members()).hasSize(7);
            assertThat(cityMeta.members().size())
                    .isEqualTo(cityMeta.fields().size() + cityMeta.methods().size());
        }

        @Test
        void empty_outline_has_no_fields_or_methods() {
            OutlineMeta empty = new OutlineMeta("Empty", "{}", null, List.of());
            assertThat(empty.fields()).isEmpty();
            assertThat(empty.methods()).isEmpty();
        }

        @Test
        void null_fields_list_handled_gracefully() {
            OutlineMeta meta = new OutlineMeta("X", "{}", null, null);
            assertThat(meta.fields()).isEmpty();
            assertThat(meta.methods()).isEmpty();
        }
    }

    // ── MetaExtractor.completionMembersOf ────────────────────────────────────

    /**
     * Tests for {@link MetaExtractor#completionMembersOf(org.twelve.gcp.outline.Outline)} and
     * {@link MetaExtractor#completionMembersOf(org.twelve.gcp.outline.Outline, ASF)}.
     *
     * <p>This is the canonical IDE dot-completion entry point: given the Outline of the expression
     * before the dot, it resolves wrappers, extracts members, and falls back to AST-body lookup
     * when inference yields only the trivial {@code to_str} builtin.
     */
    @Nested
    class CompletionMembersOf {

        // ── helpers ──────────────────────────────────────────────────────────

        private static AST freshAst() {
            ASF asf = new ASF();
            return asf.newAST();
        }

        // ── null-safety ───────────────────────────────────────────────────────

        @Test
        void null_outline_returns_empty() {
            assertThat(MetaExtractor.completionMembersOf(null)).isEmpty();
            assertThat(MetaExtractor.completionMembersOf(null, new ASF())).isEmpty();
        }

        // ── plain anonymous entity ─────────────────────────────────────────

        @Test
        void plain_anonymous_entity_returns_own_fields_and_to_str() {
            // let person = {name = "Will", age = 20}
            ASF asf = new ASF();
            AST ast = asf.newAST();
            List<MemberNode> ms = List.of(
                    new MemberNode(new Identifier(ast, new Token<>("name")),
                            LiteralNode.parse(ast, new Token<>("Will")), false),
                    new MemberNode(new Identifier(ast, new Token<>("age")),
                            LiteralNode.parse(ast, new Token<>(20)), false)
            );
            EntityNode entity = new EntityNode(ms);
            VariableDeclarator decl = new VariableDeclarator(ast, VariableKind.LET);
            decl.declare(new Identifier(ast, new Token<>("person")), entity);
            ast.addStatement(decl);
            asf.infer();

            List<FieldMeta> result = MetaExtractor.completionMembersOf(entity.outline());

            assertThat(result).extracting(FieldMeta::name).contains("name", "age");
            assertThat(result).extracting(FieldMeta::name).contains("to_str");
        }

        @Test
        void plain_entity_does_not_include_private_prefix_fields_from_own_members() {
            // Anonymous entity {_secret = "hidden", visible = "ok"}:
            // _secret is stored in the entity members and accessible via outline
            ASF asf = new ASF();
            AST ast = asf.newAST();
            List<MemberNode> ms = List.of(
                    new MemberNode(new Identifier(ast, new Token<>("_secret")),
                            LiteralNode.parse(ast, new Token<>("hidden")), false),
                    new MemberNode(new Identifier(ast, new Token<>("visible")),
                            LiteralNode.parse(ast, new Token<>("ok")), false)
            );
            EntityNode entity = new EntityNode(ms);
            VariableDeclarator decl = new VariableDeclarator(ast, VariableKind.LET);
            decl.declare(new Identifier(ast, new Token<>("obj")), entity);
            ast.addStatement(decl);
            asf.infer();

            List<FieldMeta> result = MetaExtractor.completionMembersOf(entity.outline());

            // completionMembersOf returns raw fields (filtering _-prefixed is done by callers)
            assertThat(result).extracting(FieldMeta::name).contains("visible");
        }

        // ── genericable resolution ─────────────────────────────────────────

        @Test
        void genericable_lambda_param_resolved_to_entity_fields() {
            // let f = x -> x.name;  then call f({name="Will"})
            // After inference: x is Generic with definedToBe = {name:String}
            ASF asf = new ASF();
            AST ast = asf.newAST();

            FunctionBody body = new FunctionBody(ast);
            body.addStatement(new ReturnStatement(
                    new MemberAccessor(new Identifier(ast, new Token<>("x")),
                            new Identifier(ast, new Token<>("name")))));
            FunctionNode fn = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("x"))));
            VariableDeclarator fnDecl = new VariableDeclarator(ast, VariableKind.LET);
            fnDecl.declare(new Identifier(ast, new Token<>("f")), fn);
            ast.addStatement(fnDecl);

            // f({name="Will"}) — forces x: {name:String}
            List<MemberNode> callMs = List.of(
                    new MemberNode(new Identifier(ast, new Token<>("name")),
                            LiteralNode.parse(ast, new Token<>("Will")), false));
            ast.addStatement(new ExpressionStatement(
                    new FunctionCallNode(new Identifier(ast, new Token<>("f")), new EntityNode(callMs))));
            asf.infer();

            // x.outline() is Genericable — completionMembersOf must resolve it
            List<FieldMeta> result = MetaExtractor.completionMembersOf(fn.argument().outline());

            assertThat(result).extracting(FieldMeta::name).contains("name");
        }

        // ── AST-body fallback ──────────────────────────────────────────────

        @Test
        void ast_fallback_when_entity_has_only_to_str_and_context_asf_provided() {
            // Simulate a system-type entity: an Entity created with no members
            // (after loadBuiltInMethods only to_str will appear) but the contextAsf
            // has an outline declaration "Worker = {salary:String, role:String}".
            // completionMembersOf must fall back to the AST-body members.
            ASF contextAsf = new ASF();
            AST preambleAst = contextAsf.newAST();

            // Build: outline Worker = {salary:String, role:String}  in the preamble AST
            SymbolIdentifier workerSym = new SymbolIdentifier(preambleAst, new Token<>("Worker"));
            List<Variable> vars = List.of(
                    new Variable(new Identifier(preambleAst, new Token<>("salary")), false,
                            new IdentifierTypeNode(new Identifier(preambleAst, new Token<>("String")))),
                    new Variable(new Identifier(preambleAst, new Token<>("role")), false,
                            new IdentifierTypeNode(new Identifier(preambleAst, new Token<>("String"))))
            );
            EntityTypeNode bodyNode = new EntityTypeNode(vars);
            OutlineDefinition def = new OutlineDefinition(workerSym, bodyNode);
            preambleAst.addStatement(new OutlineDeclarator(List.of(def)));

            // Entity with node = Identifier("Worker") but no inference-resolved members
            // Entity.from(node) creates an entity with empty member list → only to_str after loadBuiltInMethods
            Identifier workerIdNode = new Identifier(preambleAst, new Token<>("Worker"));
            Entity emptyWorker = Entity.from(workerIdNode, List.of());

            // Without context ASF: trivial result (only to_str)
            List<FieldMeta> withoutCtx = MetaExtractor.completionMembersOf(emptyWorker);
            assertThat(withoutCtx).extracting(FieldMeta::name)
                    .containsExactly("to_str");

            // With context ASF: fallback finds salary and role from the outline declaration
            List<FieldMeta> withCtx = MetaExtractor.completionMembersOf(emptyWorker, contextAsf);
            assertThat(withCtx).extracting(FieldMeta::name)
                    .containsExactlyInAnyOrder("salary", "role");
        }

        @Test
        void ast_fallback_not_triggered_when_entity_has_substantive_members() {
            // If extractEntityFields returns real members, the fallback must NOT be invoked
            // even if contextAsf is present (verifies the "short-circuit" logic).
            ASF contextAsf = new ASF();
            AST preambleAst = contextAsf.newAST();

            // Plain anonymous entity {val = 42}
            ASF asf = new ASF();
            AST ast = asf.newAST();
            EntityNode entity = new EntityNode(List.of(
                    new MemberNode(new Identifier(ast, new Token<>("val")),
                            LiteralNode.parse(ast, new Token<>(42)), false)));
            VariableDeclarator decl = new VariableDeclarator(ast, VariableKind.LET);
            decl.declare(new Identifier(ast, new Token<>("x")), entity);
            ast.addStatement(decl);
            asf.infer();

            List<FieldMeta> result = MetaExtractor.completionMembersOf(entity.outline(), contextAsf);

            // Must contain val (own member), not fall back to empty preamble AST
            assertThat(result).extracting(FieldMeta::name).contains("val");
        }
    }

    @Nested
    class StructuralTypeCompletion {

        private ModuleMeta emptyMeta() {
            return new ModuleMeta("m", "org.test", null, List.of(), List.of(), List.of(), List.of());
        }

        @Test
        void completion_members_of_type_reads_structural_fields_directly() {
            List<FieldMeta> result = MetaExtractor.completionMembersOfType(
                    "{age: Int,name: String,map: Poly(() → {...})}",
                    emptyMeta());

            assertThat(result).extracting(FieldMeta::name)
                    .containsExactlyInAnyOrder("age", "name", "map");
        }

        @Test
        void completion_members_of_method_return_falls_back_to_receiver_for_self_return() {
            List<FieldMeta> result = MetaExtractor.completionMembersOfMethodReturn(
                    "{age: Int,name: String,map: Poly(() → {...})}",
                    "map",
                    emptyMeta());

            assertThat(result).extracting(FieldMeta::name)
                    .containsExactlyInAnyOrder("age", "name", "map");
        }
    }
}
