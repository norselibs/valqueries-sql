package com.valqueries.automapper;

import com.valqueries.Database;
import io.ran.Clazz;
import io.ran.Key;
import io.ran.KeySet;
import com.valqueries.Database;
import com.valqueries.OrmResultSet;
import io.ran.Key;
import io.ran.KeyInfo;
import io.ran.KeySet;
import io.ran.Property;
import io.ran.TypeDescriber;
import io.ran.TypeDescriberImpl;
import io.ran.token.Token;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlGenerator {
	private SqlNameFormatter sqlNameFormatter;
	private SqlDialect dialect;
	private SqlDescriber sqlDescriber;

	@Inject
	public SqlGenerator(SqlNameFormatter sqlNameFormatter, DialectFactory dialectFactory, Database database) {
	public SqlGenerator(SqlNameFormatter sqlNameFormatter, SqlDescriber sqlDescriber) {
		this.sqlNameFormatter = sqlNameFormatter;
		this.dialect = dialectFactory.get(database);
		this.sqlDescriber = sqlDescriber;
	}

	public String getTableName(TypeDescriber<?> typeDescriber) {
		return dialect.getTableName(Clazz.of(typeDescriber.clazz()));
	}

	public String generateOrModifyTable(Database database, TypeDescriber<?> typeDescriber) {
		String tablename = getTableName(typeDescriber);
		SqlDescriber.DbTable table = sqlDescriber.describe(typeDescriber, tablename, database);
		if (table == null) {
			return generateCreateTable(typeDescriber);
		} else {

			StringBuilder sb = new StringBuilder();
			typeDescriber.fields().forEach(property -> {
				String columnName = sqlNameFormatter.column(property.getToken());
				String sqlType = getSqlType(property.getType().clazz, property);
				if (!table.getColumns().containsKey(columnName)) {
					sb.append("ALTER TABLE `" + tablename + "` ADD COLUMN `" + columnName + "` " + sqlType + ";");
				} else if (!table.getColumns().get(columnName).matches(property, sqlType)) {
					sb.append("ALTER TABLE `" + tablename + "` CHANGE COLUMN `" + columnName + "` `" + columnName + "` " + sqlType + ";");
				}
			});

			SqlDescriber.DbIndex index = table.getIndex().get("PRIMARY");
			if (!index.matches(toDbIndex(typeDescriber.primaryKeys()))) {
				sb.append("ALTER TABLE `" + tablename + "` DROP PRIMARY KEY;");
				sb.append("ALTER TABLE `" + tablename + "` ADD PRIMARY KEY(" + getPrimaryKey(typeDescriber) + ");");
			}

			typeDescriber.indexes().forEach(key -> {
				SqlDescriber.DbIndex keyIndex = toDbIndex(key);
				Optional<SqlDescriber.DbIndex> idx = table.getIndex().values().stream().filter(keyIndex::matches).findFirst();
				if (!idx.isPresent()) {
					sb.append("ALTER TABLE `" + tablename + "` ADD " + getIndex(key) + ";");
				}
			});
			return sb.toString();

		}

	}

	private SqlDescriber.DbIndex toDbIndex(KeySet index) {
		SqlDescriber.DbIndex dbIndex = new SqlDescriber.DbIndex();
		dbIndex.setUnique(index.isPrimary());
		dbIndex.setColumns(new HashMap<>());
		index.forEach(f -> {
			dbIndex.getColumns().put(f.getOrder()+1, sqlNameFormatter.column(f.getToken()));
		});
		return dbIndex;
	}

	public String generateCreateTable(TypeDescriber<?> typeDescriber) {
		return dialect.generateCreateTable(typeDescriber);
		return "CREATE TABLE IF NOT EXISTS "+ getTableName(typeDescriber)+" ("+typeDescriber.fields().stream().map(property -> {
			return "`"+sqlNameFormatter.column(property.getToken())+ "` "+getSqlType(property.getType().clazz, property);
		}).collect(Collectors.joining(", "))+", PRIMARY KEY("+ getPrimaryKey(typeDescriber) +")"+getIndexes(typeDescriber)+");";
	}

	private String getPrimaryKey(TypeDescriber<?> typeDescriber) {
		return typeDescriber.primaryKeys().stream().map(property -> {
			return "`"+sqlNameFormatter.column(property.getToken())+"`";
		}).collect(Collectors.joining(", "));
	}

	public String generateCreateTable(Class<?> clazz) {
		return generateCreateTable(TypeDescriberImpl.getTypeDescriber(clazz));
	}

	private String getIndexes(TypeDescriber<?> typeDescriber) {
		List<String> indexes = new ArrayList<>();
		for (Property property : typeDescriber.fields()) {
			Fulltext fullText = property.getAnnotations().get(Fulltext.class);
			if (fullText != null) {
				indexes.add("FULLTEXT(`"+sqlNameFormatter.column(property.getToken())+"`)");
			}
		}
		typeDescriber.indexes().forEach(keySet -> {
			if(!keySet.isPrimary()) {
				indexes.add(getIndex(keySet));
			}
		});
		if (indexes.isEmpty()) {
			return "";
		}
		return ", "+String.join(", ",indexes);
	}





}
