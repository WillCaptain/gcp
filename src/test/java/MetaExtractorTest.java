import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.twelve.gcp.meta.FieldMeta;
import org.twelve.gcp.meta.OutlineMeta;

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
}
