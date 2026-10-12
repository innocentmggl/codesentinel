package com.sentinel.application.refactor;

import java.util.List;

import com.sentinel.application.port.CodeIndex;
import com.sentinel.application.port.CodeRefactorer;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Refactoring;
import org.springframework.stereotype.Service;

@Service
public class RefactoringService {

    private final CodeIndex index;
    private final CodeRefactorer refactorer;

    RefactoringService(CodeIndex index, CodeRefactorer refactorer) {
        this.index = index;
        this.refactorer = refactorer;
    }

    /**
     * @param project indexed project to take context from; null or blank searches all projects
     */
    public Refactoring refactor(String code, String goal, String project) {
        List<CodeChunk> related = index.findRelated(code, project);
        return refactorer.refactor(code, goal, related);
    }
}
