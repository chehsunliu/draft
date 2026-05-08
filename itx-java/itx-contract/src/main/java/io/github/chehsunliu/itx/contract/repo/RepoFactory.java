package io.github.chehsunliu.itx.contract.repo;

public interface RepoFactory {
  PostRepo createPostRepo();

  UserRepo createUserRepo();

  SubscriptionRepo createSubscriptionRepo();
}
