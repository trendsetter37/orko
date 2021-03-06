package com.gruelbox.orko.db;

import static com.gruelbox.orko.db.DialectResolver.jooqDialect;
import static com.gruelbox.orko.db.DummyTable.FIELD_1;
import static com.gruelbox.orko.db.DummyTable.ID;
import static com.gruelbox.orko.db.DummyTable.TABLE_NAME;
import static org.alfasoftware.morf.metadata.DataType.STRING;
import static org.alfasoftware.morf.metadata.SchemaUtils.column;
import static org.alfasoftware.morf.metadata.SchemaUtils.index;
import static org.alfasoftware.morf.metadata.SchemaUtils.table;
import static org.jooq.impl.DSL.field;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import org.alfasoftware.morf.dataset.DataSetConnector;
import org.alfasoftware.morf.jdbc.ConnectionResources;
import org.alfasoftware.morf.jdbc.DatabaseDataSetConsumer;
import org.alfasoftware.morf.jdbc.SqlScriptExecutorProvider;
import org.alfasoftware.morf.jdbc.h2.H2;
import org.alfasoftware.morf.metadata.Table;
import org.alfasoftware.morf.upgrade.TableContribution;
import org.alfasoftware.morf.upgrade.UpgradeStep;
import org.alfasoftware.morf.xml.XmlDataSetProducer;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;

public class TestDbDump {

  @Inject private DatabaseSetup databaseSetup;
  @Inject private ConnectionResources connectionResources;
  @Inject private SqlScriptExecutorProvider sqlScriptExecutorProvider;

  @Before
  public void before() {

    DbConfiguration dbConfig = new DbConfiguration();
    dbConfig.setConnectionString("h2:mem:test;DB_CLOSE_DELAY=-1;MVCC=TRUE;DEFAULT_LOCK_TIMEOUT=60000");

    MockitoAnnotations.initMocks(this);

    Injector injector = Guice.createInjector(new DbModule(), (Module) binder -> {
      binder.bind(DbConfiguration.class).toInstance(dbConfig);
      Multibinder.newSetBinder(binder, TableContribution.class)
          .addBinding().toInstance(new TableContribution() {
            @Override
            public Collection<Table> tables() {
              return ImmutableList.of(
                  table(TABLE_NAME)
                    .columns(
                      column(ID, STRING, 45).primaryKey(),
                      column(FIELD_1, STRING, 255)
                    )
                    .indexes(
                      index(TABLE_NAME + "_1").columns(ID)
                    )
                );
            }

            @Override
            public Collection<Class<? extends UpgradeStep>> schemaUpgradeClassses() {
              return Collections.emptyList();
            }
          });
    });
    injector.injectMembers(this);
    databaseSetup.setup();
  }

  @Test
  public void testDbDump() throws IOException, SQLException {
    DataSource dataSource = connectionResources.getDataSource();
    try (Connection conn = dataSource.getConnection();
         DSLContext dsl = DSL.using(conn, jooqDialect(H2.IDENTIFIER))) {

      conn.setAutoCommit(true);

      // Set up data
      dsl.insertInto(DSL.table("DummyTable")).values(13, "TEST").execute();

      // Copy it to a temp file and back again
      File file = new DbDump(connectionResources).dump();
      try {
        new DataSetConnector(
          new XmlDataSetProducer(file.toURI().toURL()),
          new DatabaseDataSetConsumer(connectionResources, sqlScriptExecutorProvider)
        ).connect();
      } finally {
        file.delete();
      }

      // Make sure it's the same
      Result<Record2<Object, Object>> values = dsl.select(field(ID), field(FIELD_1))
          .from(DSL.table("DummyTable"))
          .fetch();
      assertEquals(1, values.size());
      assertEquals(13, values.get(0).get(field(ID), Integer.class).intValue());
      assertEquals("TEST", values.get(0).get(field(FIELD_1), String.class));
    }
  }
}
