package com.valqueries.automapper;


import io.ran.Mapper;
import io.ran.PrimaryKey;
import io.ran.Relation;

import java.util.List;

@Mapper(dbType = Valqueries.class)
public class Bike {
	@PrimaryKey
	private String id;
	private BikeType bikeType;
	private int wheelSize;
	@Relation(fields = {"bikeType", "wheelSize"}, relationFields = {"bikeType", "size"}, autoSave = true)
	private BikeWheel frontWheel;
	@Relation(fields = {"bikeType", "wheelSize"}, relationFields = {"bikeType", "size"}, autoSave = true)
	private BikeWheel backWheel;
	@Relation(fields = {"bikeType", "wheelSize"}, relationFields = {"bikeType", "size"}, autoSave = false)
	private BikeWheel auxiliaryWheel;
	@Relation(collectionElementType = BikeGear.class,via = BikeGearBike.class, autoSave = true)
	private List<BikeGear> gears;
	@Relation(collectionElementType = BikeGear.class,via = AuxiliaryBikeGearBike.class, autoSave = false)
	private List<BikeGear> auxiliaryGears;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public BikeType getBikeType() {
		return bikeType;
	}

	public void setBikeType(BikeType bikeType) {
		this.bikeType = bikeType;
	}

	public int getWheelSize() {
		return wheelSize;
	}

	public void setWheelSize(int wheelSize) {
		this.wheelSize = wheelSize;
	}

	public BikeWheel getFrontWheel() {
		return frontWheel;
	}

	public void setFrontWheel(BikeWheel frontWheel) {
		this.frontWheel = frontWheel;
	}

	public BikeWheel getBackWheel() {
		return backWheel;
	}

	public void setBackWheel(BikeWheel backWheel) {
		this.backWheel = backWheel;
	}


	public List<BikeGear> getGears() {
		return gears;
	}

	public void setGears(List<BikeGear> gears) {
		this.gears = gears;
	}

	public BikeWheel getAuxiliaryWheel() {
		return auxiliaryWheel;
	}

	public void setAuxiliaryWheel(BikeWheel auxiliaryWheel) {
		this.auxiliaryWheel = auxiliaryWheel;
	}

	public List<BikeGear> getAuxiliaryGears() {
		return auxiliaryGears;
	}

	public void setAuxiliaryGears(List<BikeGear> auxiliaryGears) {
		this.auxiliaryGears = auxiliaryGears;
	}
}
