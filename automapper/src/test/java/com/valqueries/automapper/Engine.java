package com.valqueries.automapper;

import io.ran.Mapper;
import io.ran.Relation;

import java.util.List;
import java.util.UUID;

@Mapper(dbType = Valqueries.class)
public class Engine {
	private UUID id;
	@Relation(collectionElementType = Car.class, via = EngineCar.class)
	private transient List<Car> cars;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public List<Car> getCars() {
		return cars;
	}

	public void setCars(List<Car> cars) {
		this.cars = cars;
	}
}
