import org.junit.jupiter.api.Test;
import org.twelve.gcp.ast.*;
import org.twelve.gcp.common.*;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.LiteralUnionNode;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.ArrayAccessor;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.expression.body.Body;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Consequence;
import org.twelve.gcp.node.expression.conditions.Selections;
import org.twelve.gcp.node.expression.referable.ReferenceCallNode;
import org.twelve.gcp.node.expression.referable.ReferenceNode;
import org.twelve.gcp.node.expression.typeable.*;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.function.FunctionNode;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.node.statement.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;
import org.twelve.gcp.outline.projectable.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.twelve.gcp.common.Tool.cast;

public class ASTHelper {
    public static void fillHumanAst(AST ast) {
        //namespace org.twelve.human
        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast, new Token<>("org", 10)));
        namespace.add(new Identifier(ast, new Token<>("twelve", 14)));
        namespace.add(new Identifier(ast, new Token<>("human", 21)));
        ast.program().setNamespace(namespace);

        //import grade as level, school from education;
        int offset = 22;
        List<Pair<Identifier, Identifier>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>("grade", offset + 7)), new Identifier(ast, new Token<>("level", offset + 12))));
        vars.add(new Pair<>(new Identifier(ast, new Token<>("college", offset + 15)), new Identifier(ast, new Token<>("school", offset + 21))));
        List<Identifier> source = new ArrayList<>();
        source.add(new Identifier(ast, new Token<>("education", offset + 22)));
        ast.addImport(new Import(vars, source));

        offset = 46;
        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.LET);
        //var age:Integer, name = "Will", height:Float = 1.68, my_school = school;
        //age:Integer
        var.declare(new Identifier(ast, new Token<>("age", offset + 4)), new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer"))), null);
        //name = "Will"
        var.declare(new Identifier(ast, new Token<>("name", offset + 16)), LiteralNode.parse(ast, new Token<>("Will", offset + 26)));
        //height:Float = 1.68
        var.declare(new Identifier(ast, new Token<>("height", offset + 34)), new IdentifierTypeNode(new Identifier(ast, new Token<>("Double"))), LiteralNode.parse(ast, new Token<>(1.68, offset + 50)));
        //my_school = school
        var.declare(new Identifier(ast, new Token<>("grade", offset + 52)), new Identifier(ast, new Token<>("level", offset + 64)));

        ast.program().body().addStatement(var);

        //export height as stature, name;
        offset = 100;
        vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>("height", offset)), new Identifier(ast, new Token<>("stature", offset + 11))));
        vars.add(new Pair<>(new Identifier(ast, new Token<>("name", offset + 16)), null));
        ast.addExport(new Export(vars));
    }

    public static void fillEducationAst(AST ast) {
        //namespace org.twelve.education
        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast, new Token<>("org", 10)));
        namespace.add(new Identifier(ast, new Token<>("twelve", 14)));
        namespace.add(new Identifier(ast, new Token<>("education", 21)));
        ast.program().setNamespace(namespace);

        int offset = 32;
        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.LET);
        //var grade = 1, school="NO.1";
        var.declare(new Identifier(ast, new Token<>("grade", offset)), LiteralNode.parse(ast, new Token<>(1, offset + 12)));
        var.declare(new Identifier(ast, new Token<>("school", offset)), LiteralNode.parse(ast, new Token<>("NO.1", offset + 12)));
        ast.program().body().addStatement(var);

        //export height as stature, name;
        offset = 61;
        List<Pair<Identifier, Identifier>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Identifier(ast, new Token<>("grade", offset)), null));
        vars.add(new Pair<>(new Identifier(ast, new Token<>("school", offset + 5)), new Identifier(ast, new Token<>("college", offset + 11))));
        ast.addExport(new Export(vars));
    }

    public static ASF educationAndHuman() {
        ASF asf = new ASF();
        AST education = asf.newAST();
        fillEducationAst(education);
        AST human = asf.newAST();
        fillHumanAst(human);
        return asf;
    }

    public static AST mockAddFunc() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        //const add = (x,y)->x+y;

        //List<Argument> args = new ArrayList<>();
        //x+y
        Identifier x = new Identifier(ast, new Token<>("x", 0));
        Identifier y = new Identifier(ast, new Token<>("y", 0));
        BinaryExpression add = new BinaryExpression(x, y, new OperatorNode<>(ast, BinaryOperator.ADD));

        //return x+y;
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add));

//        args.add(new Argument(ast,new Token<>("x",0)));
//        args.add(new Argument(ast,new Token<>("y",0)));
        //fn(x)->(fn(y)->x+y)
        FunctionNode addxy = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("x", 0))),
                new Argument(new Identifier(ast, new Token<>("y", 0))));


        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast, new Token<>("add", 0)), addxy);
        ast.program().body().addStatement(declare);
        return ast;
    }

    public static AST mockOverrideAddFunc() {
        AST ast = mockAddFunc();
        //x+y+z
        Identifier x = new Identifier(ast, new Token<>("x", 0));
        Identifier y = new Identifier(ast, new Token<>("y", 0));
        Identifier z = new Identifier(ast, new Token<>("z", 0));
        BinaryExpression add = new BinaryExpression(x, y, new OperatorNode<>(ast, BinaryOperator.ADD));
        add = new BinaryExpression(add, z, new OperatorNode<>(ast, BinaryOperator.ADD));

        //return x+y+z;
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(add));

//        args.add(new Argument(ast,new Token<>("x",0)));
//        args.add(new Argument(ast,new Token<>("y",0)));
        //fn(x)->(fn(y)->x+y)
        FunctionNode addxyz = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("x", 0))),
                new Argument(new Identifier(ast, new Token<>("y", 0))),
                new Argument(new Identifier(ast, new Token<>("z", 0))));
        FunctionNode addxy = cast(ast.program().body().nodes().get(0).nodes().get(0).nodes().get(1));
        ast.program().body().nodes().clear();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Identifier(ast, new Token<>("add", 0)), new PolyNode(addxy, addxyz));
        ast.program().body().addStatement(declare);

        return ast;
    }

    public static AST mockErrorPoly() {
        AST ast = mockOverrideAddFunc();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Identifier(ast, new Token<>("forError", 0)), LiteralNode.parse(ast, new Token<>("any")));
        ast.program().body().addStatement(declare);
//        Expression add1 = cast(ast.program().body().nodes().getFirst().nodes().getFirst().nodes().get(1));
//        Assignment assignment1 = new Assignment(new Identifier(ast, new Token<>("add", 0)), add1);
        Assignment assignment2 = new Assignment(new Identifier(ast, new Token<>("add", 0)), new Identifier(ast, new Token<>("forError")));
//        ast.addStatement(assignment1);
        ast.addStatement(assignment2);
        return ast;
    }

    public static AST mockSimplePersonEntity() {
        //let person: Entity = {
        //  name = "Will",
        //  get_name = ()->{
        //    this.name
        //  }
        //  get_my_name = ()->{
        //    name
        //  }
        //};
        //let name_1 = person.name;
        //let name_2 = person.get_name();
        AST ast = mockTestAst();
        List<MemberNode> members = new ArrayList<>();


        FunctionBody body = new FunctionBody(ast);
        MemberAccessor accessor = new MemberAccessor(new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("name")));
        body.addStatement(new ReturnStatement(accessor));
        members.add(new MemberNode(new Identifier(ast, new Token<>("get_name")),
                FunctionNode.from(body), false));

        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("name"))));
        members.add(new MemberNode(new Identifier(ast, new Token<>("get_my_name")),
                FunctionNode.from(body), false));
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")),
                LiteralNode.parse(ast, new Token<>("Will", 0)), false));


        EntityNode entity = new EntityNode(members);
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast, new Token<>("person", 0)), entity);
        ast.addStatement(declare);

        declare = new VariableDeclarator(ast, VariableKind.LET);
        accessor = new MemberAccessor(new Identifier(ast, new Token<>("person")), new Identifier(ast, new Token<>("name")));
        declare.declare(new Identifier(ast, new Token<>("name_1", 0)), accessor);
        ast.addStatement(declare);

        declare = new VariableDeclarator(ast, VariableKind.LET);
        accessor = new MemberAccessor(new Identifier(ast, new Token<>("person")), new Identifier(ast, new Token<>("get_name")));
        FunctionCallNode call = new FunctionCallNode(accessor);
        declare.declare(new Identifier(ast, new Token<>("name_2", 0)), call);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockSimplePersonEntityWithOverrideMember() {
        AST ast = mockSimplePersonEntity();
        FunctionBody body = new FunctionBody(ast);
//        Pair<Node, Modifier> getName = new Pair<>(FunctionNode.from(body, new Argument(ast, new Token<>("last_name"))), Modifier.PUBLIC);
        body.addStatement(new ReturnStatement(
                new BinaryExpression(
                        new MemberAccessor(new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("name"))),
                        new Identifier(ast, new Token<>("last_name")),
                        new OperatorNode<>(ast, BinaryOperator.ADD))));
        VariableDeclarator var = cast(ast.program().body().statements().getFirst());
        EntityNode person = cast(var.assignments().getFirst().rhs());
        Expression getName = person.members().get("get_name").expression();
        MemberNode node = new MemberNode(new Identifier(ast, new Token<>("get_name")),
                new PolyNode(getName, FunctionNode.from(body,
                        new Argument(new Identifier(ast, new Token<>("last_name"))))), true);
        person.nodes().remove(1);
        person.addNode(node);
        return ast;
    }

    public static AST mockInheritedPersonEntity() {
        AST ast = mockSimplePersonEntity();

        FunctionBody body = new FunctionBody(ast);
//        Pair<ONode, Modifier> getFullName = new Pair<>(FunctionNode.from(body), Modifier.PUBLIC);
        MemberAccessor name = new MemberAccessor(new Base(ast, new Token<>("base")), new Identifier(ast, new Token<>("name")));
        BinaryExpression add = new BinaryExpression(name, LiteralNode.parse(ast, new Token<>("Zhang")),
                new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add));
        MemberNode getFullName = new MemberNode(new Identifier(ast, new Token<>("get_full_name")),
                FunctionNode.from(body), false);

        List<MemberNode> members = new ArrayList<>();
        members.add(getFullName);

        body = new FunctionBody(ast);
        MemberNode getName = new MemberNode(new Identifier(ast, new Token<>("get_name")),
                FunctionNode.from(body), true);
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>("Will Zhang"))));
        members.add(getName);//重载get_name方法

        EntityNode entity = new EntityNode(members,
                ast.program().body().get(0).nodes().getFirst().nodes().getFirst());

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast, new Token<>("me", 0)), entity);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockInheritedPersonEntityWithOverrideMember() {
        AST ast = mockSimplePersonEntity();

        MemberAccessor accessor = new MemberAccessor(new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("get_name")));
        FunctionCallNode call = new FunctionCallNode(accessor);

        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ExpressionStatement(
                new BinaryExpression(
                        call,
                        new Identifier(ast, new Token<>("last_name")),
                        new OperatorNode<>(ast, BinaryOperator.ADD))));
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>(100))));
        MemberNode getName = new MemberNode(new Identifier(ast, new Token<>("get_name")),
                FunctionNode.from(body,
                        new Argument(new Identifier(ast, new Token<>("last_name")))), false);

        List<MemberNode> members = new ArrayList<>();
        members.add(getName);//重载get_name方法

        body = new FunctionBody(ast);
        call = new FunctionCallNode(new Identifier(ast, new Token<>("get_name")));
        body.addStatement(new ExpressionStatement(call));
        call = new FunctionCallNode(new Identifier(ast, new Token<>("get_name")), LiteralNode.parse(ast, new Token<>("other")));
        body.addStatement(new ReturnStatement(call));
        getName = new MemberNode(new Identifier(ast, new Token<>("get_other_name")),
                FunctionNode.from(body), false);
        members.add(getName);


        EntityNode entity = new EntityNode(members,
                ast.program().body().get(0).nodes().getFirst().nodes().getFirst());

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast, new Token<>("me", 0)), entity);
        ast.addStatement(declare);

        //me.get_name();
        accessor = new MemberAccessor(new Identifier(ast, new Token<>("me")), new Identifier(ast, new Token<>("get_name")));
        call = new FunctionCallNode(accessor, LiteralNode.parse(ast, new Token<>("Zhang")));
        ast.addStatement(new ExpressionStatement(call));
        return ast;
    }

    private static AST mockTestAst() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast, new Token<>("test")));
        ast.setNamespace(namespace);
        return ast;
    }

    public static AST mockDefinedPoly() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        PolyNode poly = new PolyNode(LiteralNode.parse(ast, new Token<>(100)), LiteralNode.parse(ast, new Token<>("some")));
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Identifier(ast, new Token<>("poly", 0)), poly);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockErrorAssignOnDefinedPoly() {
        AST ast = mockDefinedPoly();
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("poly")),
                LiteralNode.parse(ast, new Token<>(10.0f)));
        ast.addStatement(assignment);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Identifier(ast, new Token<>("poly")), LiteralNode.parse(ast, new Token<>(10.0f)));
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockDefinedLiteralUnion() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        LiteralUnionNode union = new LiteralUnionNode(LiteralNode.parse(ast, new Token<>(100)), LiteralNode.parse(ast, new Token<>("some")));
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Identifier(ast, new Token<>("union", 0)), union);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockAssignOnDefinedLiteralUnion() {
        AST ast = mockDefinedLiteralUnion();
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("union")),
                LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(assignment);
        assignment = new Assignment(new Identifier(ast, new Token<>("union")),
                LiteralNode.parse(ast, new Token<>("some")));
        ast.addStatement(assignment);
        assignment = new Assignment(new Identifier(ast, new Token<>("union")),
                LiteralNode.parse(ast, new Token<>(200)));
        ast.addStatement(assignment);
        return ast;
    }

    public static Node mockEntityProjection1(int type, Consumer<FunctionBody> mockCallNode) {
        //let f = (x,z,y)-> z.combine(x,y);
        //f(20,{combine = (x,y)->{{
        //        age = x1,
        //        name = y1.name,
        //      }},{name = "Will"})
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Identifier> namespace = new ArrayList<>();
        namespace.add(new Identifier(ast, new Token<>("test")));
        ast.setNamespace(namespace);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        Argument y = new Argument(new Identifier(ast, new Token<>("y")));
        Argument z = new Argument(new Identifier(ast, new Token<>("z")));
        FunctionBody body = new FunctionBody(ast);
        mockCallNode.accept(body);

        FunctionNode f = switch (type) {
            case 1 -> FunctionNode.from(body, x, z, y);
            case 2 -> FunctionNode.from(body, z, x, y);
            default -> FunctionNode.from(body, x, y, z);
        };
        declare.declare(new Identifier(ast, new Token<>("f")), f);
        ast.addStatement(declare);

        y = new Argument(new Identifier(ast, new Token<>("y")));
        x = new Argument(new Identifier(ast, new Token<>("x")));
        //x,y->{age=x, name = y.name}
        List<MemberNode> members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("age")), new Identifier(ast, new Token<>("x")), false));
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")), new MemberAccessor(new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("name"))), false));

        EntityNode boy = new EntityNode(members);
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(boy));

        members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("combine")), FunctionNode.from(body, x, y), false));
//        FunctionNode arg_z = FunctionNode.from(body, x, y);
        EntityNode arg_z = new EntityNode(members);

        members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")), LiteralNode.parse(ast, new Token<>("Will")), false));
        EntityNode arg_y = new EntityNode(members);
        FunctionCallNode call2 = switch (type) {
            case 1 ->
                    new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(20)), arg_z, arg_y);
            case 2 ->
                    new FunctionCallNode(new Identifier(ast, new Token<>("f")), arg_z, LiteralNode.parse(ast, new Token<>(20)), arg_y);
            default ->
                    new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(20)), arg_y, arg_z);
        };
        ast.addStatement(new ReturnStatement(call2));
        return cast(call2);
    }

    static void mockEntityProjectionNode1(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        body.addStatement(new ReturnStatement(call1));
    }

    static void mockEntityProjectionNode2(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        accessor = new MemberAccessor(call1, new Identifier(ast, new Token<>("name")));
        body.addStatement(new ReturnStatement(accessor));
    }

    static void mockEntityProjectionNode3(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        accessor = new MemberAccessor(call1, new Identifier(ast, new Token<>("gender")));
        body.addStatement(new ReturnStatement(accessor));
    }

    static void mockEntityProjectionNode4(FunctionBody body) {
        AST ast = body.ast();
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.VAR);
        declarator.declare(new Identifier(ast, new Token<>("w")), new Identifier(ast, new Token<>("z")));
        body.addStatement(declarator);
        MemberAccessor accessor = new MemberAccessor(new Identifier(ast, new Token<>("w")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        body.addStatement(new ReturnStatement(call1));
    }

    static void mockEntityProjectionNode5(FunctionBody body) {
        AST ast = body.ast();
        List<EntityMember> members = new ArrayList<>();
        members.add(EntityMember.from("name", Outline.Integer, Modifier.PUBLIC, true));
        Entity p = Entity.from(members);
        FirstOrderFunction combine = FirstOrderFunction.from(p, Outline.Integer, p);
        members = new ArrayList<>();
        members.add(EntityMember.from("combine", combine, Modifier.PUBLIC, true));
        Entity w = Entity.from(members);
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.VAR);
        declarator.declare(new Identifier(ast, new Token<>("w")), new WrapperTypeNode(ast, w), new Identifier(ast, new Token<>("z")));
        body.addStatement(declarator);
        MemberAccessor accessor = new MemberAccessor(new Identifier(ast, new Token<>("w")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        body.addStatement(new ReturnStatement(call1));
    }

    public static AST mockIf(SELECTION_TYPE selectionType) {
        ASF asf = new ASF();
        AST ast = asf.newAST();
//        VariableDeclarator declarator = new VariableDeclarator(ast,VariableKind.LET);
//        declarator.declare(new Identifier(ast,new Token<>("name")),LiteralNode.parse(ast,new Token<>("Will")));
//        ast.addStatement(declarator);
        Identifier name = new Identifier(ast, new Token<>("name"));
        Consequence c1 = new Consequence(ast);
        c1.addStatement(new ReturnStatement(name));
        Consequence c2 = new Consequence(ast);
        c2.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>("Someone"))));
        Arm arm1 = new Arm(new BinaryExpression(name,
                LiteralNode.parse(ast, new Token<>("Will")),
                new OperatorNode<>(ast, BinaryOperator.EQUALS)), c1);
        Arm arm2 = new Arm(c2);

        Selections ifs = new Selections(selectionType, arm1, arm2);

        ast.addStatement(new ExpressionStatement(ifs));
        return ast;
    }

    public static AST mockRecursive() {
        /*
        let factorial = n -> n==0?1:n*factorial(n-1);
        factorial(100);
        factorial(100);
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        Token<String> fToken = new Token<>("factorial");
        Consequence c1 = new Consequence(ast);
        c1.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>(1))));
        Consequence c2 = new Consequence(ast);
        FunctionCallNode call = new FunctionCallNode(new Identifier(ast, fToken),
                new BinaryExpression(new Identifier(ast, new Token<>("n")),
                        LiteralNode.parse(ast, new Token<>(1)),
                        new OperatorNode<>(ast, BinaryOperator.SUBTRACT)));
        c2.addStatement(new ReturnStatement(call));

        FunctionBody body = new FunctionBody(ast);
        Arm arm1 = new Arm(new BinaryExpression(new Identifier(ast, new Token<>("n")),
                LiteralNode.parse(ast, new Token<>(0)),
                new OperatorNode<>(ast, BinaryOperator.EQUALS)), c1);

        Arm arm2 = new Arm(c2);

        Selections ifs = new Selections(SELECTION_TYPE.TERNARY, arm1, arm2);
        body.addStatement(new ReturnStatement(ifs));

        FunctionNode factorial = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("n"))));
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, fToken), factorial);
        ast.addStatement(declarator);

        call = new FunctionCallNode(new Identifier(ast, fToken), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call));

        call = new FunctionCallNode(new Identifier(ast, fToken), LiteralNode.parse(ast, new Token<>("100")));
        ast.addStatement(new ExpressionStatement(call));

        return ast;
    }

    public static AST mockOptionIsAs() {
        /*let result = {
            var some:String|Integer = "string";
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        };*/
        ASF asf = new ASF();
        AST ast = asf.newAST();
        Block block = new Block(ast);
        //var some:String|Integer = 100;
        Token<String> some = new Token<>("some");
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.VAR);

        declarator.declare(new Identifier(ast, some), new WrapperTypeNode(ast, Option.from(Outline.String, Outline.Integer)), LiteralNode.parse(ast, new Token<>("string")));
        block.addStatement(declarator);
        mockIsAs(ast, some, block);
        //let result = {...}
        Token<String> result = new Token<>("result");
        declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, result), block);
        ast.addStatement(declarator);
        return ast;
    }

    private static void mockIsAs(AST ast, Token<String> some, Body body) {
        //if(some is Integer){
        //    some
        //}else if(some is String as str){
        //    str
        //}else{100}
        IsAs isInt = new IsAs(new Identifier(ast, some), Outline.Integer);
        Consequence consequence = new Consequence(ast);
        consequence.addStatement(new ReturnStatement(new Identifier(ast, some)));
        Arm arm1 = new Arm(isInt, consequence);
        Token<String> str = new Token<>("str");
        IsAs isStr = new IsAs(new Identifier(ast, some), Outline.String, new Identifier(ast, str));
        consequence = new Consequence(ast);
        consequence.addStatement(new ReturnStatement(new Identifier(ast, str)));
        Arm arm2 = new Arm(isStr, consequence);
        consequence = new Consequence(ast);
        consequence.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>(100))));
        Arm arm3 = new Arm(consequence);
        Selections ifs = new Selections(SELECTION_TYPE.IF, arm1, arm2, arm3);
        body.addStatement(new ReturnStatement(ifs));
    }

    public static AST mockAs() {
        /*
        let a = {name="Will",age = 20} as {name:String};
        let b = {name="Will",age = 20} as {name:Integer};
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        VariableDeclarator aDeclare = new VariableDeclarator(ast, VariableKind.LET);
        Expression exp1 = new As(mockSimpleEntity(ast), mockSimpleEntityType(ast, "String"));
        aDeclare.declare(new Identifier(ast, new Token<>("a")), exp1);
        ast.addStatement(aDeclare);
        VariableDeclarator bDeclare = new VariableDeclarator(ast, VariableKind.LET);
        Expression exp2 = new As(mockSimpleEntity(ast), mockSimpleEntityType(ast, "Integer"));
        bDeclare.declare(new Identifier(ast, new Token<>("b")), exp2);
        ast.addStatement(bDeclare);
        return ast;
    }

    private static EntityNode mockSimpleEntity(AST ast) {
        List<MemberNode> members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")), LiteralNode.parse(ast, new Token<>("Will")), false));
        members.add(new MemberNode(new Identifier(ast, new Token<>("age")), LiteralNode.parse(ast, new Token<>(20)), false));
        return new EntityNode(members);
    }

    private static EntityTypeNode mockSimpleEntityType(AST ast, String type) {
        List<Variable> members = new ArrayList<>();
        members.add(new Variable(new Identifier(ast, new Token<>("name")), false,
                new IdentifierTypeNode(new Identifier(ast, new Token<>(type)))));
        return new EntityTypeNode(members);
    }

    public static AST mockPolyIsAs() {
        /*let result = {
            var some = 100&"string";
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        };*/
        ASF asf = new ASF();
        AST ast = asf.newAST();

        Block block = new Block(ast);

        //var some = 100&"some";
        Token<String> some = new Token<>("some");
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.VAR);
        declarator.declare(new Identifier(ast, some), new PolyNode(LiteralNode.parse(ast, new Token<>(100)),
                new PolyNode(LiteralNode.parse(ast, new Token<>("some")))));
        block.addStatement(declarator);
        mockIsAs(ast, some, block);
        //let result = {...}
        Token<String> result = new Token<>("result");
        declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, result), block);
        ast.addStatement(declarator);
        return ast;
    }

    public static AST mockGenericIsAs() {
         /*let result = some->{
            if(some is Integer){
                some
            }else if(some is String as str){
                str
            }else{100}
        }(100);*/
        ASF asf = new ASF();
        AST ast = asf.newAST();

        Token<String> some = new Token<>("some");
        FunctionBody body = new FunctionBody(ast);
        mockIsAs(ast, some, body);

        FunctionNode function = new FunctionNode(new Argument(new Identifier(ast, some), new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer")))), body);
        FunctionCallNode call = new FunctionCallNode(function, LiteralNode.parse(ast, new Token<>(100)));

        //let result = some->{...}
        Token<String> result = new Token<>("result");
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, result), call);
        ast.addStatement(declarator);

        return ast;
    }

    public static AST mockReferenceInFunction() {
        /*
        let f = fx<a,b>(x:a)->{
           let y:b = 100;
           y
        }*/
        ASF asf = new ASF();
        AST ast = asf.newAST();
        FunctionBody body = new FunctionBody(ast);
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, new Token<>("y")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("b"))),
                LiteralNode.parse(ast, new Token<>(100)));
        body.addStatement(declarator);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("y"))));

        List<ReferenceNode> refs = new ArrayList<>();
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("a")), null));
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("b")), null));
        List<Argument> args = new ArrayList<>();
        args.add(new Argument(new Identifier(ast, new Token<>("x")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("a")))));
        FunctionNode func = FunctionNode.from(body, refs, args);
        declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, new Token<>("f")), func);
        ast.addStatement(declarator);
        return ast;
    }

    public static AST mockDeclare() {
        /*
         let f = (x:String->Integer->{name:String,age:Integer},y:String,z:Integer)->x(y,z);
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        //{name:String,age:Integer}
        List<Variable> members = new ArrayList<>();
        members.add(new Variable(new Identifier(ast, new Token<>("name")), false, new IdentifierTypeNode(new Identifier(ast, new Token<>("String")))));
        members.add(new Variable(new Identifier(ast, new Token<>("age")), true, new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer")))));
        EntityTypeNode entityTypeNode = new EntityTypeNode(members);

        //String->Integer->{name:String,age:Integer}
        FunctionTypeNode functionTypeNode = new FunctionTypeNode(ast, entityTypeNode,
                new WrapperTypeNode(ast, Outline.String), new WrapperTypeNode(ast, Outline.Integer));
        //x:...
        Argument x = new Argument(new Identifier(ast, new Token<>("x")), functionTypeNode);
        //y:String
        Argument y = new Argument(new Identifier(ast, new Token<>("y")), new WrapperTypeNode(ast, Outline.String));
        //z:Integer
        Argument z = new Argument(new Identifier(ast, new Token<>("z")), new WrapperTypeNode(ast, Outline.Integer));
        //x(y,z)
        FunctionBody body = new FunctionBody(ast);
        FunctionCallNode call = new FunctionCallNode(new Identifier(ast, new Token<>("x")),
                new Identifier(ast, new Token<>("y")),
                new Identifier(ast, new Token<>("z")));
        body.addStatement(new ReturnStatement(call));
        //(x,y,z)->x(y,z)
        FunctionNode f = FunctionNode.from(body, x, y, z);

        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, new Token<>("f")), f);
        ast.addStatement(declarator);
        return ast;
    }

    public static AST mockReferenceInEntity() {
        /*
        let g = fx<a,b>()->{
           {
                z:a = 100,
                f = fx<c>(x:b,y:c)->y
            }
        }*/
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<MemberNode> members = new ArrayList<>();
        //z:a=100
        members.add(new MemberNode(new Identifier(ast, new Token<>("z")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("a"))),
                LiteralNode.parse(ast, new Token<>(100)), false));
        //f = fx<c>(x:b,y:c)->y
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("y"))));
        List<ReferenceNode> refs = new ArrayList<>();
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("c")), null));
        List<Argument> args = new ArrayList<>();
        args.add(new Argument(new Identifier(ast, new Token<>("x")), new IdentifierTypeNode(new Identifier(ast, new Token<>("b")))));
        args.add(new Argument(new Identifier(ast, new Token<>("y")), new IdentifierTypeNode(new Identifier(ast, new Token<>("c")))));
        FunctionNode f = FunctionNode.from(body, refs, args);
        members.add(new MemberNode(new Identifier(ast, new Token<>("f")), f, false));
        EntityNode entity = new EntityNode(members, null);

        //<a,b>
        refs = new ArrayList<>();
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("a")), null));
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("b")), null));

        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(entity));
        FunctionNode g = FunctionNode.from(body, refs, new ArrayList<>());
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(new Identifier(ast, new Token<>("g")), g);
        ast.addStatement(declarator);
        return ast;
    }

    public static AST mockArrayDefinition() {
        /*
         * let a = [1,2,3,4];
         * let b:[String]= [];
         * let c = [...5];//[0,1,2,3,4]
         * let d = [1...6,2,x->x*2];//1 to 6, step 2 *2, [2,6,10]
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        //let a = [1,2,3,4];
        Expression[] items = new Expression[4];
        items[0] = LiteralNode.parse(ast, new Token<>(1));
        items[1] = LiteralNode.parse(ast, new Token<>(2));
        items[2] = LiteralNode.parse(ast, new Token<>(3));
        items[3] = LiteralNode.parse(ast, new Token<>(4));
        ArrayNode array_a = new ArrayNode(ast, items);
        VariableDeclarator adeclare = new VariableDeclarator(ast, VariableKind.LET);
        adeclare.declare(new Identifier(ast, new Token<>("a")), array_a);
        ast.addStatement(adeclare);
        //let b:[String]= [];
        ArrayNode array_b = new ArrayNode(ast);
        VariableDeclarator bdeclare = new VariableDeclarator(ast, VariableKind.LET);
        bdeclare.declare(new Identifier(ast, new Token<>("b")),
                new ArrayTypeNode(ast, new IdentifierTypeNode(new Identifier(ast, new Token<>("String")))),
                array_b);
        ast.addStatement(bdeclare);
        //let c = [...5];
        ArrayNode array_c = new ArrayNode(ast, null,
                LiteralNode.parse(ast, new Token<>(5)), null, null, null);
        VariableDeclarator cdeclare = new VariableDeclarator(ast, VariableKind.LET);
        cdeclare.declare(new Identifier(ast, new Token<>("c")), new ArrayTypeNode(ast), array_c);
        ast.addStatement(cdeclare);
        //let d = [1...6,2,x->x*2,x->x%2==0];
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new BinaryExpression(
                new Identifier(ast, new Token<>("x")), LiteralNode.parse(ast, new Token<>("2")),
                new OperatorNode<>(ast, BinaryOperator.ADD))));
        FunctionNode processor = new FunctionNode(new Argument(new Identifier(ast, new Token<>("x"))), body);

        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new BinaryExpression(
                new BinaryExpression(new Identifier(ast, new Token<>("x")),
                        LiteralNode.parse(ast, new Token<>(2)), new OperatorNode<>(ast, BinaryOperator.MODULUS)),
                LiteralNode.parse(ast, new Token<>(0)),
                new OperatorNode<>(ast, BinaryOperator.EQUALS))));
        FunctionNode condition = new FunctionNode(new Argument(new Identifier(ast, new Token<>("x"))), body);
        ArrayNode array_d = new ArrayNode(ast, LiteralNode.parse(ast, new Token<>(1)),
                LiteralNode.parse(ast, new Token<>(6)),
                LiteralNode.parse(ast, new Token<>(2)), processor, condition);
        VariableDeclarator ddeclare = new VariableDeclarator(ast, VariableKind.LET);
        ddeclare.declare(new Identifier(ast, new Token<>("d")), array_d);
        ast.addStatement(ddeclare);
        return ast;
    }

    public static AST mockArrayMethods() {
        /*
         * let a = [1,2],b =[3,4];
         * let c = a.concat(b);
         * let d = c.slice(1,3);
         * let i = d.indexof(2);//index
         * let yes = d.includes(4);//true of false
         * b.push(a[0]);
         * b.unshift(a.shift());
         * let str = b.join(", ");
         * d.reverse();
         * d.sort((a,b)->a>b)
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        return ast;
    }

    public static AST mockArrayStreamOperations() {
        /*
         * let a = [1,2,3,4];
         * let c = a.map(i->i*2);
         * let d = a.filter(i->i%2==0);
         * let e = a.reduce((acc,i)->acc+i,0);
         * let f = a.find(i->i>2);
         * let g = a.indexof(i->i>3);
         * let yes = a.contains(i->i>0);
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        return ast;
    }

    public static AST mockArrayAsArgument() {
        /*
         * let f = x->x[0];
         * let g = (x:[],i)->{
         *     y = x[i];
         *     x = ["will","zhang"];
         *     y
         * };
         * let r = <a>(x:[a])->{
         *     let b = [1,2];
         *     b = x;
         *     let c:a = x[0];
         *     c
         * }
         * f([{name:"Will"}]);//correct call
         * f(100);//wrong call
         * g(["a","b"],0);//correct
         * g([1,2],"idx");//wrong
         * let r1 = r<Integer>;
         * r1([1,2]);//correct
         * let r2 = r<String>;//wrong reference
         * r([1,2]);//wrong
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        //let f = x->x[0];
        VariableDeclarator f = new VariableDeclarator(ast, VariableKind.LET);
        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new ArrayAccessor(ast, new Identifier(ast, new Token<>("x")),
                LiteralNode.parse(ast, new Token<>(0)))));
        FunctionNode fDef = new FunctionNode(new Argument(new Identifier(ast, new Token<>("x"))), body);
        f.declare(new Identifier(ast, new Token<>("f")), fDef);
        ast.addStatement(f);
        //let g = (x:[],i)->{
        //    let y = x[i];
        //    x = ["will","zhang"];
        //    y
        // };
        VariableDeclarator g = new VariableDeclarator(ast, VariableKind.LET);
        body = new FunctionBody(ast);
        VariableDeclarator y = new VariableDeclarator(ast, VariableKind.LET);
        y.declare(new Identifier(ast, new Token<>("y")),
                new ArrayAccessor(ast, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("i"))));
        body.addStatement(y);
        Expression[] values = new Expression[2];
        values[0] = LiteralNode.parse(ast, new Token<>("will"));
        values[1] = LiteralNode.parse(ast, new Token<>("zhang"));
        Assignment xassign = new Assignment(new Identifier(ast, new Token<>("x")),
                new ArrayNode(ast, values));
        body.addStatement(xassign);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("y"))));
        FunctionNode gDef = FunctionNode.from(body, new Argument(new Identifier(ast, new Token<>("x")), new ArrayTypeNode(ast)),
                new Argument(new Identifier(ast, new Token<>("i"))));
        g.declare(new Identifier(ast, new Token<>("g")), gDef);
        ast.addStatement(g);
        //let r = <a>(x:[a])->{
        //   var b = [1,2];
        //   b = x;
        //   let c:a = x[0];
        //   c
        //}
        VariableDeclarator r = new VariableDeclarator(ast, VariableKind.LET);
        List<ReferenceNode> refs = new ArrayList<>();
        refs.add(new ReferenceNode(new Identifier(ast, new Token<>("a")), null));
        List<Argument> args = new ArrayList<>();
        args.add(new Argument(new Identifier(ast, new Token<>("x")),
                new ArrayTypeNode(ast, new IdentifierTypeNode(new Identifier(ast, new Token<>("a"))))));
        body = new FunctionBody(ast);
        VariableDeclarator b = new VariableDeclarator(ast, VariableKind.VAR);
        values = new Expression[2];
        values[0] = LiteralNode.parse(ast, new Token<>(1));
        values[1] = LiteralNode.parse(ast, new Token<>(2));
        b.declare(new Identifier(ast, new Token<>("b")), new ArrayNode(ast, values));
        body.addStatement(b);
        Assignment bassign = new Assignment(new Identifier(ast, new Token<>("b")), new Identifier(ast, new Token<>("x")));
        body.addStatement(bassign);
        VariableDeclarator c = new VariableDeclarator(ast, VariableKind.LET);
        c.declare(new Identifier(ast, new Token<>("c")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("a"))),
                new ArrayAccessor(ast, new Identifier(ast, new Token<>("x")), LiteralNode.parse(ast, new Token<>(0))));
        body.addStatement(c);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("c"))));
        FunctionNode rDef = FunctionNode.from(body, refs, args);
        r.declare(new Identifier(ast, new Token<>("r")), rDef);
        ast.addStatement(r);
        //f([{name:"Will"}]);
        values = new Expression[1];
        List<MemberNode> members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")), LiteralNode.parse(ast, new Token<>("Will")), false));
        values[0] = new EntityNode(members);
        ArrayNode farray_1 = new ArrayNode(ast, values);
        FunctionCallNode fcall_1 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), farray_1);
        ast.addStatement(new ExpressionStatement(fcall_1));
        //f(100);//wrong call
        Expression fcall_2 = new FunctionCallNode(new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(100)));
        ast.addStatement(new ExpressionStatement(fcall_2));
        //g(["a","b"],0);//correct
        values = new Expression[2];
        values[0] = LiteralNode.parse(ast, new Token<>("a"));
        values[1] = LiteralNode.parse(ast, new Token<>("b"));
        Expression gcall_1 = new FunctionCallNode(new Identifier(ast, new Token<>("g")),
                new ArrayNode(ast, values), LiteralNode.parse(ast, new Token<>(0)));
        ast.addStatement(new ExpressionStatement(gcall_1));
        // g([1],"idx");//wrong
        values = new Expression[1];
        values[0] = LiteralNode.parse(ast, new Token<>(1));
        Expression gcall_2 = new FunctionCallNode(new Identifier(ast, new Token<>("g")),
                new ArrayNode(ast, values), LiteralNode.parse(ast, new Token<>("idx")));
        ast.addStatement(new ExpressionStatement(gcall_2));
        //let r1 = r<Integer>;
        VariableDeclarator r1 = new VariableDeclarator(ast, VariableKind.LET);
        Expression rrefcall = new ReferenceCallNode(new Identifier(ast, new Token<>("r")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer"))));
        r1.declare(new Identifier(ast, new Token<>("r1")), rrefcall);
        ast.addStatement(r1);
        //r1([1,2]);//correct
        values = new Expression[2];
        values[0] = LiteralNode.parse(ast, new Token<>(1));
        values[1] = LiteralNode.parse(ast, new Token<>(2));
        Expression r1call_1 = new FunctionCallNode(new Identifier(ast, new Token<>("r1")), new ArrayNode(ast, values));
        ast.addStatement(new ExpressionStatement(r1call_1));
        //let r2 = r<String>;//wrong reference
        VariableDeclarator r2 = new VariableDeclarator(ast, VariableKind.LET);
        rrefcall = new ReferenceCallNode(new Identifier(ast, new Token<>("r")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("String"))));
        r2.declare(new Identifier(ast, new Token<>("r2")), rrefcall);
        ast.addStatement(r2);
        //r([1,2]);
        values = new Expression[2];
        values[0] = LiteralNode.parse(ast, new Token<>(1));
        values[1] = LiteralNode.parse(ast, new Token<>(2));
        Expression rcall = new FunctionCallNode(new Identifier(ast, new Token<>("r")), new ArrayNode(ast, values));
        ast.addStatement(new ExpressionStatement(rcall));
        return ast;
    }

    public static AST mockArrayComplicatedAssign() {
        /**
         * let f = (x,y)->{
         *   ....todo
         * }
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();

        return ast;
    }

    public static AST mockDeclaredHofProjection() {
        /*
         * let f = fx<a>(x:a->{name:?,age:Integer})->{
         *   x("Will").name
         * }
         * f<Integer>;
         * f(n->{name=n,age=30})
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        VariableDeclarator fDecl = new VariableDeclarator(ast, VariableKind.LET);
        List<Variable> members = new ArrayList<>();
        members.add(new Variable(new Identifier(ast, new Token<>("name")), false,
                new IdentifierTypeNode(new Identifier(ast, new Token<>("a")))));
        members.add(new Variable(new Identifier(ast, new Token<>("age")), false,
                new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer")))));
        TypeNode xDef = new FunctionTypeNode(ast, new EntityTypeNode(members),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("a"))));
        Argument arg = new Argument(new Identifier(ast, new Token<>("x")), xDef);

        FunctionBody body = new FunctionBody(ast);
        Expression call = new FunctionCallNode(new Identifier(ast, new Token<>("x")),
                LiteralNode.parse(ast, new Token<>("Will")));
        Expression name = new MemberAccessor(call, new Identifier(ast, new Token<>("name")));
        body.addStatement(new ReturnStatement(name));
        Expression f = new FunctionNode(arg, body, new ReferenceNode(new Identifier(ast, new Token<>("a")), null));
        fDecl.declare(new Identifier(ast, new Token<>("f")), f);
        ast.addStatement(fDecl);

        //f<Integer>;
        Expression rCall = new ReferenceCallNode(new Identifier(ast, new Token<>("f")),
                new IdentifierTypeNode(new Identifier(ast, new Token<>("Integer"))));
        ast.addStatement(new ExpressionStatement(rCall));
        //f(n->{name=n,age=30})
        body = new FunctionBody(ast);
        List<MemberNode> ms = new ArrayList<>();
        ms.add(new MemberNode(new Identifier(ast, new Token<>("name")),
                new Identifier(ast, new Token<>("n")), false));
        ms.add(new MemberNode(new Identifier(ast, new Token<>("age")),
                LiteralNode.parse(ast, new Token<>(30)), false));
        body.addStatement(new ReturnStatement(new EntityNode(ms)));
        Expression someone = new FunctionNode(new Argument(new Identifier(ast, new Token<>("n"))), body);
        call = new FunctionCallNode(new Identifier(ast, new Token<>("f")), someone);
        ast.addStatement(new ReturnStatement(call));
        return ast;
    }

    public static AST mockExtendHofProjection() {
        /*
         * let f = x->{
         *   x = a->{name=a,age=10};
         *   x("Will").name
         * }
         * f(n->{name=n})
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        VariableDeclarator fDel = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(new Identifier(ast, new Token<>("x")));
        FunctionBody fbody = new FunctionBody(ast);
        FunctionBody abody = new FunctionBody(ast);
        List<MemberNode> members = new ArrayList<>();
        members.add(new MemberNode(new Identifier(ast, new Token<>("name")),
                new Identifier(ast, new Token<>("a")), false));
        members.add(new MemberNode(new Identifier(ast, new Token<>("age")),
                LiteralNode.parse(ast, new Token<>(10)), false));
        Expression entity = new EntityNode(members);
        abody.addStatement(new ReturnStatement(entity));
        Expression a = new FunctionNode(new Argument(new Identifier(ast, new Token<>("a"))), abody);
        fbody.addStatement(new Assignment(new Identifier(ast, new Token<>("x")), a));

        Expression f = new FunctionNode(x, fbody);

        Expression call = new FunctionCallNode(new Identifier(ast, new Token<>("x")),
                LiteralNode.parse(ast, new Token<>("Will")));
        Expression name = new MemberAccessor(call, new Identifier(ast, new Token<>("name")));
        fbody.addStatement(new ReturnStatement(name));
        fDel.declare(new Identifier(ast, new Token<>("f")), f);
        ast.addStatement(fDel);
        //f(n->{name=n})
        FunctionBody body = new FunctionBody(ast);
        List<MemberNode> ms = new ArrayList<>();
        ms.add(new MemberNode(new Identifier(ast, new Token<>("name")),
                new Identifier(ast, new Token<>("n")), false));
//        ms.add(new MemberNode(new Identifier(ast, new Token<>("age")),
//                LiteralNode.parse(ast, new Token<>(30)), false));
        body.addStatement(new ReturnStatement(new EntityNode(ms)));
        Expression someone = new FunctionNode(new Argument(new Identifier(ast, new Token<>("n"))), body);
        call = new FunctionCallNode(new Identifier(ast, new Token<>("f")), someone);
        ast.addStatement(new ReturnStatement(call));
        return ast;
    }

    public static AST mockComplicatedHofProjection() {
        /*
         * let f = fx<a>(x:a->a,y,z)->{
         *   x = b->b+"some";
         *   y = x;
         *   x = z;
         *   y
         * };
         * f<String>;
         * f<Integer>;
         * f(n->"some");
         */
        ASF asf = new ASF();
        AST ast = asf.newAST();
        VariableDeclarator fDel = new VariableDeclarator(ast,VariableKind.LET);
        FunctionBody body = new FunctionBody(ast);
        FunctionBody bodyb = new FunctionBody(ast);
        bodyb.addStatement(new ReturnStatement(new BinaryExpression(
                new Identifier(ast,new Token<>("b")),LiteralNode.parse(ast,new Token<>("some")),
                new OperatorNode<>(ast,BinaryOperator.ADD))));
        Expression fb = FunctionNode.from(bodyb,new Argument(new Identifier(ast,new Token<>("b"))));
        Statement f2b = new Assignment(new Identifier(ast,new Token<>("x")),fb);
        body.addStatement(f2b);
        body.addStatement(new Assignment(new Identifier(ast,new Token<>("y")),
                        new Identifier(ast,new Token<>("x"))));
        body.addStatement(new Assignment(new Identifier(ast,new Token<>("x")),
                new Identifier(ast,new Token<>("z"))));
        body.addStatement(new ReturnStatement(new Identifier(ast,new Token<>("y"))));
        List<ReferenceNode> refs = new ArrayList<>();
        refs.add(new ReferenceNode(new Identifier(ast,new Token<>("a")),null));
        List<Argument> args = new ArrayList<>();
        TypeNode type = new FunctionTypeNode(ast,new IdentifierTypeNode(new Identifier(ast,new Token<>("a"))),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("a"))));
        args.add(new Argument(new Identifier(ast,new Token<>("x")),type));
        args.add(new Argument(new Identifier(ast,new Token<>("y"))));
        args.add(new Argument(new Identifier(ast,new Token<>("z"))));
        Expression f = FunctionNode.from(body,refs,args);
        fDel.declare(new Identifier(ast,new Token<>("f")),f);
        ast.addStatement(fDel);
        ast.addStatement(new ExpressionStatement(new ReferenceCallNode(
                new Identifier(ast,new Token<>("f")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("String"))))));
        ast.addStatement(new ExpressionStatement(new ReferenceCallNode(
                new Identifier(ast,new Token<>("f")),
                new IdentifierTypeNode(new Identifier(ast,new Token<>("Integer"))))));
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast,new Token<>("some"))));
        ast.addStatement(new ExpressionStatement(new FunctionCallNode(
                new Identifier(ast,new Token<>("f")),
                FunctionNode.from(body,new Argument(new Identifier(ast,new Token<>("n")))))));
        return ast;
    }
}
