package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;

import com.example.sneaker.model.*;
import java.nio.file.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;

/** Export DDL with the real SQL Server dialect without connecting to a user's database. */
class SqlServerSchemaExportTest {
  @Test
  void exportMappedSchema() throws Exception {
    Path output = Path.of("target/sqlserver-schema.sql");
    Files.createDirectories(output.getParent());
    try (var writer = Files.newBufferedWriter(output)) {
      Map<String, Object> settings = new HashMap<>();
      settings.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
      settings.put("hibernate.boot.allow_jdbc_metadata_access", false);
      settings.put("hibernate.use_nationalized_character_data", true);
      settings.put(
          "hibernate.physical_naming_strategy",
          CamelCaseToUnderscoresNamingStrategy.class.getName());
      settings.put("hibernate.default_schema", "dbo");
      settings.put("jakarta.persistence.schema-generation.database.action", "none");
      settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
      settings.put("jakarta.persistence.schema-generation.scripts.create-target", writer);
      settings.put("hibernate.hbm2ddl.delimiter", ";");
      var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
      try {
        var sources = new MetadataSources(registry);
        for (var type :
            List.of(
                Category.class,
                Product.class,
                ProductVariant.class,
                CustomerOrder.class,
                OrderItem.class,
                Promotion.class,
                PosOperator.class,
                PosDraft.class,
                PosLine.class,
                PosScanEvent.class)) sources.addAnnotatedClass(type);
        for (var type :
            List.of(
                StaffAccount.class,
                Customer.class,
                CustomerAddress.class,
                ShoeAttribute.class,
                MediaAsset.class,
                ProductColorImage.class,
                OrderEvent.class,
                PaymentAttempt.class,
                EmailChallenge.class,
                OrderRefund.class,
                ShopAccount.class,
                ShopSession.class)) sources.addAnnotatedClass(type);
        var metadata = sources.buildMetadata();
        SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> {});
      } finally {
        StandardServiceRegistryBuilder.destroy(registry);
      }
    }
    String sql = Files.readString(output);
    assertThat(sql)
        .contains(
            "create table dbo.products",
            "nvarchar(180)",
            "identity",
            "create table dbo.pos_draft_invoices");
    assertThat(sql.lines().filter(l -> l.startsWith("create table ")).count()).isEqualTo(24);
  }
}
