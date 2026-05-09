package io.github.chehsunliu.itx.contract.repo;

public class RepoNotFoundException extends RuntimeException {
    public RepoNotFoundException() {
        super("not found");
    }
}
