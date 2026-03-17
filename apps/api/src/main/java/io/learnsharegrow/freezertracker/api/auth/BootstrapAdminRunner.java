package io.learnsharegrow.freezertracker.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(BootstrapAdminRunner.class);

  private final BootstrapAdminService bootstrapAdminService;
  private final ConfigurableApplicationContext context;
  private final boolean enabled;
  private final boolean exitAfterRun;
  private final String username;
  private final String password;
  private final String houseName;

  public BootstrapAdminRunner(
      BootstrapAdminService bootstrapAdminService,
      ConfigurableApplicationContext context,
      @Value("${bootstrap.admin.enabled:false}") boolean enabled,
      @Value("${bootstrap.admin.exit-after-run:true}") boolean exitAfterRun,
      @Value("${bootstrap.admin.username:}") String username,
      @Value("${bootstrap.admin.password:}") String password,
      @Value("${bootstrap.admin.house-name:Admin House}") String houseName) {
    this.bootstrapAdminService = bootstrapAdminService;
    this.context = context;
    this.enabled = enabled;
    this.exitAfterRun = exitAfterRun;
    this.username = username;
    this.password = password;
    this.houseName = houseName;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }

    BootstrapAdminResult result = bootstrapAdminService.bootstrapAdmin(username, password, houseName);
    logger.info(
        "Bootstrap admin completed (accountId={}, accountCreated={}, houseId={}, houseCreated={})",
        result.accountId(),
        result.accountCreated(),
        result.houseId(),
        result.houseCreated());

    if (exitAfterRun) {
      logger.info("Bootstrap admin run complete, shutting down application by configuration");
      SpringApplication.exit(context, () -> 0);
    }
  }
}
