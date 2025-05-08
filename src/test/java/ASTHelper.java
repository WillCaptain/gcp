import org.twelve.gcp.ast.ASF;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.common.*;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.node.LiteralUnionNode;
import org.twelve.gcp.node.expression.*;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Consequence;
import org.twelve.gcp.node.expression.conditions.Selections;
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
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.twelve.gcp.common.Tool.cast;

public class ASTHelper {
    public static void fillHumanAst(AST ast) {
        //namespace org.twelve.human
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("org", 10));
        namespace.add(new Token<>("twelve", 14));
        namespace.add(new Token<>("human", 21));
        ast.program().setNamespace(namespace);

        //import grade as level, school from education;
        int offset = 22;
        List<Pair<Token<String>, Token<String>>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Token<>("grade", offset + 7), new Token<>("level", offset + 12)));
        vars.add(new Pair<>(new Token<>("college", offset + 15), new Token<>("school", offset + 21)));
        List<Token<String>> source = new ArrayList<>();
        source.add(new Token<>("education", offset + 22));
        ast.addImport(new Import(ast, vars, source));

        offset = 46;
        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.LET);
        //var age:Integer, name = "Will", height:Float = 1.68, my_school = school;
        //age:Integer
        var.declare(new Token<>("age", offset + 4), ProductADT.Integer);
        //name = "Will"
        var.declare(new Token<>("name", offset + 19), LiteralNode.parse(ast, new Token<>("Will", offset + 26)));
        //height:Float = 1.68
        var.declare(new Token<>("height", offset + 34), ProductADT.Double, LiteralNode.parse(ast, new Token<>(1.68, offset + 50)));
        //my_school = school
        var.declare(new Token<>("grade", offset + 52), new Identifier(ast, new Token<>("level", offset + 64)));

        ast.program().body().addStatement(var);

        //export height as stature, name;
        offset = 100;
        vars = new ArrayList<>();
        vars.add(new Pair<>(new Token<>("height", offset), new Token<>("stature", offset + 11)));
        vars.add(new Pair<>(new Token<>("name", offset + 16), null));
        ast.addExport(new Export(ast, vars));
    }

    public static void fillEducationAst(AST ast) {
        //namespace org.twelve.education
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("org", 10));
        namespace.add(new Token<>("twelve", 14));
        namespace.add(new Token<>("education", 21));
        ast.program().setNamespace(namespace);

        int offset = 32;
        VariableDeclarator var = new VariableDeclarator(ast, VariableKind.LET);
        //var grade = 1, school="NO.1";
        var.declare(new Token<>("grade", offset), LiteralNode.parse(ast, new Token<>(1, offset + 12)));
        var.declare(new Token<>("school", offset), LiteralNode.parse(ast, new Token<>("NO.1", offset + 12)));
        ast.program().body().addStatement(var);

        //export height as stature, name;
        offset = 61;
        List<Pair<Token<String>, Token<String>>> vars = new ArrayList<>();
        vars.add(new Pair<>(new Token<>("grade", offset), null));
        vars.add(new Pair<>(new Token<>("school", offset + 5), new Token<>("college", offset + 11)));
        ast.addExport(new Export(ast, vars));
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
        FunctionNode addxy = FunctionNode.from(body, new Argument(ast, new Token<>("x", 0)), new Argument(ast, new Token<>("y", 0)));


        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("add", 0), addxy);
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
        FunctionNode addxyz = FunctionNode.from(body, new Argument(ast, new Token<>("x", 0)),
                new Argument(ast, new Token<>("y", 0)), new Argument(ast, new Token<>("z", 0)));
        FunctionNode addxy = cast(ast.program().body().nodes().get(0).nodes().get(0).nodes().get(1));
        ast.program().body().nodes().clear();

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Token<>("add", 0), new PolyNode(ast,addxy,addxyz));
        ast.program().body().addStatement(declare);

        return ast;
    }

    public static AST mockErrorPoly() {
        AST ast = mockOverrideAddFunc();
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Token<>("forError", 0), LiteralNode.parse(ast, new Token<>("any")));
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
        MemberAccessor accessor = new MemberAccessor(ast, new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("name")));
        body.addStatement(new ReturnStatement(accessor));
        members.add(new MemberNode(ast, new Token<>("get_name"),
                FunctionNode.from(body), false));

        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(new Identifier(ast, new Token<>("name"))));
        members.add(new MemberNode(ast, new Token<>("get_my_name"),
                FunctionNode.from(body), false));
        members.add(new MemberNode(ast, new Token<>("name"),
                LiteralNode.parse(ast, new Token<>("Will", 0)), false));


        EntityNode entity = new EntityNode(ast, members);
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("person", 0), entity);
        ast.addStatement(declare);

        declare = new VariableDeclarator(ast, VariableKind.LET);
        accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("person")), new Identifier(ast, new Token<>("name")));
        declare.declare(new Token<>("name_1", 0), accessor);
        ast.addStatement(declare);

        declare = new VariableDeclarator(ast, VariableKind.LET);
        accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("person")), new Identifier(ast, new Token<>("get_name")));
        FunctionCallNode call = new FunctionCallNode(ast, accessor);
        declare.declare(new Token<>("name_2", 0), call);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockSimplePersonEntityWithOverrideMember() {
        AST ast = mockSimplePersonEntity();
        FunctionBody body = new FunctionBody(ast);
//        Pair<Node, Modifier> getName = new Pair<>(FunctionNode.from(body, new Argument(ast, new Token<>("last_name"))), Modifier.PUBLIC);
        body.addStatement(new ReturnStatement(
                new BinaryExpression(
                        new MemberAccessor(ast, new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("name"))),
                        new Identifier(ast, new Token<>("last_name")),
                        new OperatorNode<>(ast, BinaryOperator.ADD))));
        VariableDeclarator var = cast(ast.program().body().statements().getFirst());
        EntityNode person = cast(var.assignments().getFirst().rhs());
        Expression getName = person.members().get("get_name").expression();
        MemberNode node = new MemberNode(ast, new Token<>("get_name"),
                new PolyNode(ast,getName,FunctionNode.from(body, new Argument(ast, new Token<>("last_name")))), true);
        person.nodes().remove(1);
        person.addNode(node);
        return ast;
    }

    public static AST mockInheritedPersonEntity() {
        AST ast = mockSimplePersonEntity();

        FunctionBody body = new FunctionBody(ast);
//        Pair<ONode, Modifier> getFullName = new Pair<>(FunctionNode.from(body), Modifier.PUBLIC);
        MemberAccessor name = new MemberAccessor(ast, new Base(ast, new Token<>("base")), new Identifier(ast, new Token<>("name")));
        BinaryExpression add = new BinaryExpression(name, LiteralNode.parse(ast, new Token<>("Zhang")),
                new OperatorNode<>(ast, BinaryOperator.ADD));
        body.addStatement(new ReturnStatement(add));
        MemberNode getFullName = new MemberNode(ast, new Token<>("get_full_name"),
                FunctionNode.from(body), false);

        List<MemberNode> members = new ArrayList<>();
        members.add(getFullName);

        body = new FunctionBody(ast);
        MemberNode getName = new MemberNode(ast, new Token<>("get_name"),
                FunctionNode.from(body), true);
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>("Will Zhang"))));
        members.add(getName);//重载get_name方法

        EntityNode entity = new EntityNode(ast, members,
                ast.program().body().get(0).nodes().getFirst().nodes().getFirst());

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("me", 0), entity);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockInheritedPersonEntityWithOverrideMember() {
        AST ast = mockSimplePersonEntity();

        MemberAccessor accessor = new MemberAccessor(ast, new This(ast, new Token<>("this")), new Identifier(ast, new Token<>("get_name")));
        FunctionCallNode call = new FunctionCallNode(ast, accessor);

        FunctionBody body = new FunctionBody(ast);
        body.addStatement(new ExpressionStatement(
                new BinaryExpression(
                        call,
                        new Identifier(ast, new Token<>("last_name")),
                        new OperatorNode<>(ast, BinaryOperator.ADD))));
        body.addStatement(new ReturnStatement(LiteralNode.parse(ast,new Token<>(100))));
        MemberNode getName = new MemberNode(ast, new Token<>("get_name"),
                FunctionNode.from(body, new Argument(ast, new Token<>("last_name"))), false);

        List<MemberNode> members = new ArrayList<>();
        members.add(getName);//重载get_name方法

        body = new FunctionBody(ast);
        call = new FunctionCallNode(ast, new Identifier(ast,new Token<>("get_name")));
        body.addStatement(new ExpressionStatement(call));
        call = new FunctionCallNode(ast, new Identifier(ast,new Token<>("get_name")),LiteralNode.parse(ast,new Token<>("other")));
        body.addStatement(new ReturnStatement(call));
        getName = new MemberNode(ast, new Token<>("get_other_name"),
                FunctionNode.from(body), false);
        members.add(getName);


        EntityNode entity = new EntityNode(ast, members,
                ast.program().body().get(0).nodes().getFirst().nodes().getFirst());

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("me", 0), entity);
        ast.addStatement(declare);

        //me.get_name();
        accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("me")), new Identifier(ast, new Token<>("get_name")));
        call = new FunctionCallNode(ast,accessor,LiteralNode.parse(ast,new Token<>("Zhang")));
        ast.addStatement(new ExpressionStatement(call));
        return ast;
    }

    private static AST mockTestAst() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("test"));
        ast.setNamespace(namespace);
        return ast;
    }

    public static AST mockDefinedPoly() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        PolyNode poly = new PolyNode(ast, LiteralNode.parse(ast, new Token<>(100)), LiteralNode.parse(ast, new Token<>("some")));
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Token<>("poly", 0), poly);
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockErrorAssignOnDefinedPoly() {
        AST ast = mockDefinedPoly();
        Assignment assignment = new Assignment(new Identifier(ast, new Token<>("poly")),
                LiteralNode.parse(ast, new Token<>(10.0f)));
        ast.addStatement(assignment);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        declare.declare(new Token<>("poly"), LiteralNode.parse(ast, new Token<>(10.0f)));
        ast.addStatement(declare);
        return ast;
    }

    public static AST mockDefinedLiteralUnion() {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        LiteralUnionNode union = new LiteralUnionNode(ast, LiteralNode.parse(ast, new Token<>(100)), LiteralNode.parse(ast, new Token<>("some")));
        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.VAR);
        declare.declare(new Token<>("union", 0), union);
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
        List<Token<String>> namespace = new ArrayList<>();
        namespace.add(new Token<>("test"));
        ast.setNamespace(namespace);

        VariableDeclarator declare = new VariableDeclarator(ast, VariableKind.LET);
        Argument x = new Argument(ast, new Token<>("x"));
        Argument y = new Argument(ast, new Token<>("y"));
        Argument z = new Argument(ast, new Token<>("z"));
        FunctionBody body = new FunctionBody(ast);
        mockCallNode.accept(body);

        FunctionNode f = switch (type) {
            case 1 -> FunctionNode.from(body, x, z, y);
            case 2 -> FunctionNode.from(body, z, x, y);
            default -> FunctionNode.from(body, x, y, z);
        };
        declare.declare(new Token<>("f"), f);
        ast.addStatement(declare);

        y = new Argument(ast, new Token<>("y"));
        x = new Argument(ast, new Token<>("x"));
        //x,y->{age=x, name = y.name}
        List<MemberNode> members = new ArrayList<>();
        members.add(new MemberNode(ast, new Token<>("age"), new Identifier(ast, new Token<>("x")), false));
        members.add(new MemberNode(ast, new Token<>("name"), new MemberAccessor(ast, new Identifier(ast, new Token<>("y")), new Identifier(ast, new Token<>("name"))), false));

        EntityNode boy = new EntityNode(ast, members);
        body = new FunctionBody(ast);
        body.addStatement(new ReturnStatement(boy));

        members = new ArrayList<>();
        members.add(new MemberNode(ast, new Token<>("combine"), FunctionNode.from(body, x, y), false));
//        FunctionNode arg_z = FunctionNode.from(body, x, y);
        EntityNode arg_z = new EntityNode(ast, members);

        members = new ArrayList<>();
        members.add(new MemberNode(ast, new Token<>("name"), LiteralNode.parse(ast, new Token<>("Will")), false));
        EntityNode arg_y = new EntityNode(ast, members);
        FunctionCallNode call2 = switch (type) {
            case 1 ->
                    new FunctionCallNode(ast, new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(20)), arg_z, arg_y);
            case 2 ->
                    new FunctionCallNode(ast, new Identifier(ast, new Token<>("f")), arg_z, LiteralNode.parse(ast, new Token<>(20)), arg_y);
            default ->
                    new FunctionCallNode(ast, new Identifier(ast, new Token<>("f")), LiteralNode.parse(ast, new Token<>(20)), arg_y, arg_z);
        };
        ast.addStatement(new ReturnStatement(call2));
        return cast(call2);
    }

    static void mockEntityProjectionNode1(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(ast, accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        body.addStatement(new ReturnStatement(call1));
    }

    static void mockEntityProjectionNode2(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(ast, accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        accessor = new MemberAccessor(body.ast(), call1, new Identifier(ast, new Token<>("name")));
        body.addStatement(new ReturnStatement(accessor));
    }

    static void mockEntityProjectionNode3(FunctionBody body) {
        AST ast = body.ast();
        MemberAccessor accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("z")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(ast, accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        accessor = new MemberAccessor(ast, call1, new Identifier(ast, new Token<>("gender")));
        body.addStatement(new ReturnStatement(accessor));
    }

    static void mockEntityProjectionNode4(FunctionBody body) {
        AST ast = body.ast();
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.VAR);
        declarator.declare(new Token<>("w"), new Identifier(ast, new Token<>("z")));
        body.addStatement(declarator);
        MemberAccessor accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("w")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(ast, accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
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
        declarator.declare(new Token<>("w"), w, new Identifier(ast, new Token<>("z")));
        body.addStatement(declarator);
        MemberAccessor accessor = new MemberAccessor(ast, new Identifier(ast, new Token<>("w")), new Identifier(ast, new Token<>("combine")));
        FunctionCallNode call1 = new FunctionCallNode(ast, accessor, new Identifier(ast, new Token<>("x")), new Identifier(ast, new Token<>("y")));
        body.addStatement(new ReturnStatement(call1));
    }

    public static AST mockIf(SELECTION_TYPE selectionType) {
        ASF asf = new ASF();
        AST ast = asf.newAST();
        Identifier name = new Identifier(ast, new Token<>("name"), Outline.String, false);
        Consequence c1 = new Consequence(ast);
        c1.addStatement(new ReturnStatement(name));
        Consequence c2 = new Consequence(ast);
        c2.addStatement(new ReturnStatement(LiteralNode.parse(ast, new Token<>("Someone"))));
        Arm arm1 = new Arm(ast, new BinaryExpression(name,
                LiteralNode.parse(ast, new Token<>("Will")),
                new OperatorNode<>(ast, BinaryOperator.EQUALS)), c1);
        Arm arm2 = new Arm(ast, c2);

        Selections ifs = new Selections(ast, null, selectionType, arm1, arm2);

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
        FunctionCallNode call = new FunctionCallNode(ast,fToken ,
                new BinaryExpression(new Identifier(ast,new Token<>("n")),
                LiteralNode.parse(ast,new Token<>(1)),
                        new OperatorNode<>(ast,BinaryOperator.SUBTRACT)));
        c2.addStatement(new ReturnStatement(call));

        FunctionBody body = new FunctionBody(ast);
        Arm arm1 = new Arm(ast, new BinaryExpression(new Identifier(ast, new Token<>("n")),
                LiteralNode.parse(ast, new Token<>(0)),
                new OperatorNode<>(ast, BinaryOperator.EQUALS)), c1);

        Arm arm2 = new Arm(ast, c2);

        Selections ifs = new Selections(ast, null, SELECTION_TYPE.TERNARY, arm1, arm2);
        body.addStatement(new ReturnStatement(ifs));

        FunctionNode factorial = FunctionNode.from(body, new Argument(ast, new Token<>("n")));
        VariableDeclarator declarator = new VariableDeclarator(ast, VariableKind.LET);
        declarator.declare(fToken, factorial);
        ast.addStatement(declarator);

        call = new FunctionCallNode(ast,fToken,LiteralNode.parse(ast,new Token<>(100)));
        ast.addStatement(new ExpressionStatement(call));

        call = new FunctionCallNode(ast,fToken,LiteralNode.parse(ast,new Token<>("100")));
        ast.addStatement(new ExpressionStatement(call));

        return ast;
    }

}
