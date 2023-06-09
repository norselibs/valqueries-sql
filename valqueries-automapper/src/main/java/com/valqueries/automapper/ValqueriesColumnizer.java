/* Copyright (C) Persequor ApS - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Persequor Development Team <partnersupport@persequor.com>,
 */
package com.valqueries.automapper;

import com.valqueries.IStatement;
import com.valqueries.Setter;
import io.ran.*;
import io.ran.token.Token;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ValqueriesColumnizer<T> implements ObjectMapColumnizer, Setter {
	private final List<Consumer<IStatement>> statements = new ArrayList<>();
	private final Set<String> sqlStatements = new LinkedHashSet<>();
	private final Set<String> sqlWithoutKey = new LinkedHashSet<>();
	protected final Map<String,String> fields = new LinkedHashMap<>();
	protected final Map<String, String> fieldsWithoutKeys = new LinkedHashMap<>();
	protected final Set<String> keys = new LinkedHashSet<>();
	protected TypeDescriber<? extends Object> typeDescriber;
	CompoundKey key;
	protected SqlDialect dialect;
	protected SqlNameFormatter sqlNameFormatter;


	public ValqueriesColumnizer(GenericFactory factory, MappingHelper mappingHelper, T t, SqlNameFormatter columnFormatter,SqlDialect dialect) {
		this.sqlNameFormatter = columnFormatter;
		this.dialect = dialect;
		key = mappingHelper.getKey(t);
		typeDescriber = TypeDescriberImpl.getTypeDescriber(t.getClass());
		mappingHelper.columnize(t, this);
	}

	protected ValqueriesColumnizer() {}



	protected void add(Property property, Consumer<IStatement> consumer) {
		fields.put(property.getSnakeCase(),transformKey(property));

		String sql = "`"+transformKey(property)+"` = :"+property.getSnakeCase();

		if (((Property.PropertyValueList<?>)this.key.getValues()).stream().anyMatch(pv -> {
			return !pv.getProperty().getSnakeCase().equals(property.getSnakeCase());
		})) {
			fieldsWithoutKeys.put(property.getSnakeCase(),transformKey(property));
			sqlWithoutKey.add(sql);
		} else {
			keys.add(transformKey(property));
		}
		statements.add(consumer);
	}

	protected String transformKey(Property key) {
		return dialect.column(key).toSql();
	}

	protected String transformFieldPlaceholder(Property key) {
		return key.getSnakeCase();
	}

	@Override
	public void set(Property key, String value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property property, Character character) {
		add(property, s -> s.set(transformFieldPlaceholder(property), character == null ? null : character.toString()));
	}

	@Override
	public void set(Property key, ZonedDateTime value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property property, LocalDateTime localDateTime) {
		add(property, s -> s.set(transformFieldPlaceholder(property),localDateTime));
	}

	@Override
	public void set(Property property, Instant instant) {
		add(property, s -> s.set(transformFieldPlaceholder(property), instant));
	}

	@Override
	public void set(Property property, LocalDate localDate) {
		add(property, s -> s.set(transformFieldPlaceholder(property), localDate));
	}

	@Override
	public void set(Property property, LocalTime localTime) {
		add(property, s -> s.set(transformFieldPlaceholder(property), localTime));
	}

	@Override
	public void set(Property key, Integer value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property property, Short aShort) {
		add(property, s -> s.set(transformFieldPlaceholder(property), aShort == null ? null : aShort.intValue()));
	}

	@Override
	public void set(Property key, Long value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property key, UUID value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property key, Double value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property key, BigDecimal value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value == null ? null : value.toString()));
	}

	@Override
	public void set(Property key, Float value) {
		add(key, s -> s.set(transformFieldPlaceholder(key), value));
	}

	@Override
	public void set(Property property, Boolean value) {
		add(property, s -> s.set(transformFieldPlaceholder(property), value));
	}

	@Override
	public void set(Property property, Byte aByte) {
		add(property, s -> s.set(transformFieldPlaceholder(property), aByte == null ? null : aByte.intValue()));
	}

	@Override
	public void set(Property property, byte[] bytes) {
		add(property, s -> s.set(transformFieldPlaceholder(property), bytes));
	}

	@Override
	public void set(Property property, Enum<?> anEnum) {
		add(property, s -> s.set(transformFieldPlaceholder(property), anEnum));
	}

	@Override
	public void set(Property property, Collection<?> list) {
		add(property, s -> s.set(transformFieldPlaceholder(property), list == null ? null : list.stream().map(Object::toString).collect(Collectors.joining(","))));
	}

	@Override
	public void set(IStatement statement) {
		statements.forEach(s -> s.accept(statement));
	}

		public String getSqlWithoutKey() {
		return String.join(", ", sqlWithoutKey);
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public Set<String> getKeys() {
		return keys;
	}

	public Map<String,String> getFieldsWithoutKeys() {
		return fieldsWithoutKeys;
	}
}