package com.example.sneaker;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import java.sql.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** DDL/DML smoke test on H2 MSSQL mode, NOT a substitute for native SQL Server execution. */
class SqlCatalogDataTest {
  @Test
  void exportedSchemaAndGeneratedCatalogDmlPreserveExistingInventory() throws Exception {
    String schema = Files.readString(Path.of("../database/01_schema.sql"));
    String seed = Files.readString(Path.of("../database/02_real_products.sql"));
    String ddl =
        schema
            .split("-- BEGIN HIBERNATE SQL SERVER DDL\\R", 2)[1]
            .split("-- END HIBERNATE SQL SERVER DDL", 2)[0];
    try (var connection =
            DriverManager.getConnection(
                "jdbc:h2:mem:sql_catalog_smoke;MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE", "sa", "");
        var sql = connection.createStatement()) {
      sql.execute("CREATE SCHEMA dbo");
      // H2 has no DATETIMEOFFSET alias; only the test adapter maps its timezone type.
      String compatible =
          ddl.replaceAll("(?i)datetimeoffset\\((\\d+)\\)", "timestamp($1) with time zone");
      for (String statement : compatible.split(";"))
        if (!statement.isBlank()) sql.execute(statement);
      assertThat(
              number(
                  sql, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='dbo'"))
          .isEqualTo(24);
      sql.execute(table(seed, "Products", "seed_products"));
      sql.execute(table(seed, "Variants", "seed_variants"));
      sql.execute(insert(seed, "@Products").replace("@Products", "seed_products"));
      sql.execute(insert(seed, "@Variants").replace("@Variants", "seed_variants"));
      sql.execute("CREATE TABLE seed_inserted(id bigint,code nvarchar(40))");
      sql.execute("CREATE TABLE seed_missing(code nvarchar(40))");
      // SQL Server OUTPUT is emulated by capturing missing codes before INSERT on H2 only.
      sql.execute("INSERT INTO dbo.categories(name,active) VALUES(N'Existing user category',1)");
      sql.execute(
          "INSERT INTO"
              + " dbo.products(code,name,brand,price,description,image_url,hover_image_url,active,featured,category_id)"
              + " SELECT N'USER-001',N'Existing"
              + " product',N'User',1,N'Keep',N'/existing.jpg',N'/existing.jpg',1,0,id FROM"
              + " dbo.categories WHERE name=N'Existing user category'");
      sql.execute(
          "INSERT INTO dbo.product_variants(product_id,sku,color,shoe_size,price,stock,active)"
              + " SELECT id,N'SKU-USER',N'User color',N'40',1,7,1 FROM dbo.products WHERE"
              + " code=N'USER-001'");
      apply(seed, sql);
      assertThat(number(sql, "SELECT COUNT(*) FROM dbo.products WHERE code LIKE 'F6S-RC-%'"))
          .isEqualTo(25);
      assertThat(number(sql, "SELECT COUNT(*) FROM dbo.product_variants WHERE sku LIKE 'SKU-RC%'"))
          .isEqualTo(250);
      assertThat(
              number(sql, "SELECT SUM(stock) FROM dbo.product_variants WHERE sku LIKE 'SKU-RC%'"))
          .isEqualTo(2250);
      assertThat(number(sql, "SELECT stock FROM dbo.product_variants WHERE sku='SKU-USER'"))
          .isEqualTo(7);
      assertThat(
              number(sql, "SELECT COUNT(*) FROM dbo.products WHERE name=N'Nike Air Force 1 ''07'"))
          .isEqualTo(1);
      sql.execute(
          "UPDATE dbo.product_variants SET stock=3,price=123456 WHERE sku='SKU-RC001-BLK-38'");
      apply(seed, sql);
      assertThat(number(sql, "SELECT stock FROM dbo.product_variants WHERE sku='SKU-RC001-BLK-38'"))
          .isEqualTo(3);
      assertThat(number(sql, "SELECT price FROM dbo.product_variants WHERE sku='SKU-RC001-BLK-38'"))
          .isEqualTo(123456);
      assertThat(number(sql, "SELECT COUNT(*) FROM dbo.product_variants")).isEqualTo(251);
      assertThat(seed)
          .contains(
              "catalog_import_history",
              "BEGIN TRANSACTION",
              "ROLLBACK TRANSACTION",
              "SERIALIZABLE");
    }
  }

  private static String table(String seed, String variable, String name) {
    var m = Pattern.compile("DECLARE @" + variable + " TABLE\\(([^;]+)\\);").matcher(seed);
    assertThat(m.find()).isTrue();
    return "CREATE TABLE " + name + "(" + m.group(1) + ")";
  }

  private static String insert(String seed, String table) {
    int start = seed.indexOf("INSERT INTO " + table);
    assertThat(start).isGreaterThanOrEqualTo(0);
    return seed.substring(start, seed.indexOf(';', start));
  }

  private static String replaceTables(String value) {
    return value
        .replace("@Products", "seed_products")
        .replace("@Variants", "seed_variants")
        .replace("@Inserted", "seed_inserted");
  }

  private static void apply(String seed, Statement sql) throws Exception {
    sql.execute("DELETE FROM seed_inserted");
    sql.execute("DELETE FROM seed_missing");
    sql.execute(
        "INSERT INTO seed_missing SELECT code FROM seed_products WHERE NOT EXISTS(SELECT 1 FROM"
            + " dbo.products p WHERE p.code=seed_products.code)");
    sql.execute(replaceTables(insert(seed, "dbo.categories")));
    String product =
        insert(seed, "dbo.products")
            .replaceAll("OUTPUT inserted\\.id,inserted\\.code INTO @Inserted\\(id,code\\)", "");
    sql.execute(replaceTables(product));
    sql.execute(
        "INSERT INTO seed_inserted SELECT id,code FROM dbo.products WHERE code IN(SELECT code FROM"
            + " seed_missing)");
    sql.execute(replaceTables(insert(seed, "dbo.product_variants")));
  }

  private static long number(Statement sql, String query) throws Exception {
    try (var result = sql.executeQuery(query)) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }
}
