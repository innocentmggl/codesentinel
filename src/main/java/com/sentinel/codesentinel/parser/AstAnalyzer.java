package com.sentinel.codesentinel.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPublicModifier;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.sentinel.codesentinel.domain.ClassMetrics;
import com.sentinel.codesentinel.domain.MethodMetric;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AstAnalyzer {

    static {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);  // newest syntax JavaParser supports
    }

    public ParsedSource parse(String source) {
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(source);
        } catch (ParseProblemException e) {
            throw new IllegalArgumentException(
                    "Not valid Java source: " + e.getProblems().getFirst().getMessage(), e);
        }

        String className = cu.getTypes().stream()
                .filter(NodeWithPublicModifier::isPublic)
                .findFirst()
                .or(() -> cu.getTypes().stream().findFirst())
                .map(NodeWithSimpleName::getNameAsString)
                .orElse("Unknown");
        String packageName = cu.getPackageDeclaration()
                .map(PackageDeclaration::getNameAsString).orElse("");
        List<String> imports = cu.getImports().stream()
                .map(ImportDeclaration::getNameAsString).toList();

        List<MethodMetric> methods = cu.findAll(MethodDeclaration.class).stream()
                .map(m -> new MethodMetric(
                        m.getNameAsString(),
                        m.getBegin().map(p -> p.line).orElse(-1),
                        m.getEnd().map(p -> p.line).orElse(-1),
                        complexity(m)))
                .toList();

        // Strip comments to cut tokens. Metrics were computed above, so order matters.
        cu.getAllContainedComments().forEach(Comment::remove);
        String condensed = cu.toString();

        return new ParsedSource(
                new ClassMetrics(className, packageName, imports, methods),
                condensed, source.length(), condensed.length());
    }

    private int complexity(MethodDeclaration method) {
        int complexity = 1;
        complexity += method.findAll(IfStmt.class).size();
        complexity += method.findAll(ForStmt.class).size();
        complexity += method.findAll(ForEachStmt.class).size();
        complexity += method.findAll(WhileStmt.class).size();
        complexity += method.findAll(DoStmt.class).size();
        complexity += method.findAll(CatchClause.class).size();
        complexity += method.findAll(ConditionalExpr.class).size();
        complexity += (int) method.findAll(SwitchEntry.class).stream()
                .filter(e -> !e.getLabels().isEmpty()).count();
        complexity += (int) method.findAll(BinaryExpr.class).stream()
                .filter(b -> b.getOperator() == BinaryExpr.Operator.AND
                        || b.getOperator() == BinaryExpr.Operator.OR).count();
        return complexity;
    }
}