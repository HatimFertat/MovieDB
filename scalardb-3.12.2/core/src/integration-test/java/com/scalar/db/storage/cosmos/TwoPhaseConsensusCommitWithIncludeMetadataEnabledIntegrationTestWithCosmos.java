package com.scalar.db.storage.cosmos;

import com.scalar.db.transaction.consensuscommit.TwoPhaseConsensusCommitWithIncludeMetadataEnabledIntegrationTestBase;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class TwoPhaseConsensusCommitWithIncludeMetadataEnabledIntegrationTestWithCosmos
    extends TwoPhaseConsensusCommitWithIncludeMetadataEnabledIntegrationTestBase {

  @Override
  protected Properties getProperties(String testName) {
    return CosmosEnv.getProperties(testName);
  }

  @Override
  protected String getNamespace() {
    String namespace = super.getNamespace();
    Optional<String> databasePrefix = CosmosEnv.getDatabasePrefix();
    return databasePrefix.map(prefix -> prefix + namespace).orElse(namespace);
  }

  @Override
  protected Map<String, String> getCreationOptions() {
    return CosmosEnv.getCreationOptions();
  }
}
