package com.valqueries.automapper;

import com.google.inject.Injector;
import com.valqueries.Database;
import io.ran.GenericFactory;
import io.ran.Resolver;
import io.ran.TypeDescriber;
import io.ran.TypeDescriberImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public abstract class AutoMapperBaseTests {
	static Database database;
	static Injector injector;
	static GenericFactory factory;
	static TypeDescriber<Car> carDescriber;
	static TypeDescriber<Door> doorDescriber;
	static TypeDescriber<Engine> engineDescriber;
	static TypeDescriber<EngineCar> engineCarDescriber;
	static TypeDescriber<Tire> tireDescriber;
	static TypeDescriber<WithCollections> withCollectionsDescriber;
	static TypeDescriber<Bike> bikeDescriber;
	static TypeDescriber<BikeGear> bikeGearDescriber;
	static TypeDescriber<BikeGearBike> bikeGearBikeDescriber;
	static TypeDescriber<BikeWheel> bikeWheelDescriber;
	static TypeDescriber<PrimaryKeyModel> primaryKeyDescriber;

	@Mock
	Resolver resolver;
	CarRepository carRepository;
	DoorRepository doorRepository;
	SqlGenerator sqlGenerator;
	private EngineRepository engineRepository;
	TypeDescriber<Exhaust> exhaustDescriber;
	ExhaustRepository exhaustRepository;
	TireRepository tireRepository;
	WithCollectionsRepository withCollectionsRepository;
	BikeRepository bikeRepository;
	AllFieldTypesRepository allFieldTypesRepository;
	PrimaryKeyModelRepository primayKeyModelRepository;
	BipodRepository podRepository;


	@Before
	public void setupBase() {
		setInjector();
		sqlGenerator = injector.getInstance(SqlGenerator.class);
		carDescriber = TypeDescriberImpl.getTypeDescriber(Car.class);
		doorDescriber = TypeDescriberImpl.getTypeDescriber(Door.class);
		engineDescriber = TypeDescriberImpl.getTypeDescriber(Engine.class);
		engineCarDescriber = TypeDescriberImpl.getTypeDescriber(EngineCar.class);
		exhaustDescriber = TypeDescriberImpl.getTypeDescriber(Exhaust.class);
		tireDescriber = TypeDescriberImpl.getTypeDescriber(Tire.class);
		withCollectionsDescriber = TypeDescriberImpl.getTypeDescriber(WithCollections.class);
		primaryKeyDescriber = TypeDescriberImpl.getTypeDescriber(PrimaryKeyModel.class);

		bikeDescriber = TypeDescriberImpl.getTypeDescriber(Bike.class);
		bikeGearDescriber = TypeDescriberImpl.getTypeDescriber(BikeGear.class);
		bikeGearBikeDescriber = TypeDescriberImpl.getTypeDescriber(BikeGearBike.class);
		bikeWheelDescriber = TypeDescriberImpl.getTypeDescriber(BikeWheel.class);
		withCollectionsDescriber = TypeDescriberImpl.getTypeDescriber(WithCollections.class);


		carRepository = injector.getInstance(CarRepository.class);
		doorRepository = injector.getInstance(DoorRepository.class);
		engineRepository = injector.getInstance(EngineRepository.class);
		exhaustRepository = injector.getInstance(ExhaustRepository.class);
		tireRepository = injector.getInstance(TireRepository.class);
		withCollectionsRepository = injector.getInstance(WithCollectionsRepository.class);
		bikeRepository = injector.getInstance(BikeRepository.class);
		primayKeyModelRepository = injector.getInstance(PrimaryKeyModelRepository.class);
		podRepository = injector.getInstance(BipodRepository.class);
		allFieldTypesRepository = injector.getInstance(AllFieldTypesRepository.class);
	}

	protected abstract void setInjector();

	@After
	public void resetDb() {

	}

	@Test
	public void happy() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setBrand(Brand.Porsche);
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Optional<Car> actualOptional = carRepository.get(model.getId());
		Car actual = actualOptional.orElseThrow(RuntimeException::new);
		assertEquals(model.getId(), actual.getId());
		assertEquals(model.getTitle(), actual.getTitle());
		assertEquals(model.getCreatedAt(), actual.getCreatedAt());
		assertEquals(Brand.Porsche, actual.getBrand());
	}

	@Test
	public void lazyLoad() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Door lazyModel = factory.get(Door.class);
		lazyModel.setId(UUID.randomUUID());
		lazyModel.setTitle("Lazy as such");
		lazyModel.setCar(model);
		doorRepository.save(lazyModel);

		Door lazyModelToo = factory.get(Door.class);
		lazyModelToo.setId(UUID.randomUUID());
		lazyModelToo.setTitle("Lazy as well");
		lazyModelToo.setCar(model);
		doorRepository.save(lazyModelToo);

		Door actualLazy = doorRepository.get(lazyModel.getId()).orElseThrow(RuntimeException::new);
		Car actual = actualLazy.getCar();
		assertEquals(model.getId(), actual.getId());
		assertEquals(model.getTitle(), actual.getTitle());
		assertEquals(model.getCreatedAt(), actual.getCreatedAt());
		assertEquals(2, actual.getDoors().size());
	}

	@Test
	public void queryBuilder() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh 2");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);


		Collection<Car> cars = carRepository.query()
				.eq(Car::getTitle,"Muh 2")
				.execute()
				.collect(Collectors.toList());

		assertEquals(1, cars.size());
	}

	@Test
	public void queryBuilder_subQuery() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Door door = factory.get(Door.class);
		door.setCarId(model.getId());
		door.setTitle("Door title");
		door.setId(UUID.randomUUID());
		doorRepository.save(door);


		Collection<Door> doors = doorRepository.query()
				.subQuery(Door::getCar
						, query -> query.eq(Car::getTitle, "Muh")
				)
				.execute().collect(Collectors.toList());
		assertEquals(1, doors.size());
	}

	@Test
	public void queryBuilder_subQuery_in() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Door door = factory.get(Door.class);
		door.setCarId(model.getId());
		door.setTitle("Door title");
		door.setId(UUID.randomUUID());
		doorRepository.save(door);


		Collection<Door> doors = doorRepository.query()
				.subQuery(Door::getCar
						, query -> query.in(Car::getTitle, Arrays.asList("Muh"))
				)
				.execute().collect(Collectors.toList());
		assertEquals(1, doors.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void queryBuilder_subQuery_in_withEmptyArray_throwsIllegalArgument() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Door door = factory.get(Door.class);
		door.setCarId(model.getId());
		door.setTitle("Door title");
		door.setId(UUID.randomUUID());
		doorRepository.save(door);

		doorRepository.query()
				.subQuery(Door::getCar
						, query -> query.in(Car::getTitle, Collections.emptyList())
				).execute();
	}

	@Test
	public void queryBuilder_subQueryVia() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Engine engine = factory.get(Engine.class);
		engine.setId(UUID.randomUUID());
		engine.setCars(new ArrayList<>());
		engine.getCars().add(model);
		engineRepository.save(engine);


		Collection<Engine> doors = engineRepository.query()
				.subQueryList(Engine::getCars
						, query -> query.eq(Car::getTitle, "Muh")
				)
				.execute().collect(Collectors.toList());
		assertEquals(1, doors.size());
	}

	@Test
	public void queryBuilder_inQuery() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh 2");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh 3");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.in(Car::getTitle, "Muh 2", "Muh")
				.execute().collect(Collectors.toList());
		assertEquals(2, cars.size());
		verify(resolver, never()).getTypedCollection(any(), any(),any(), any());
		verify(resolver, never()).getCollection(any(), any(), any());
		verify(resolver, never()).get(any(), any(), any());
	}

	@Test
	public void queryBuilder_sortAndLimit() {
		Car model1 = factory.get(Car.class);
		model1.setId(UUID.randomUUID());
		model1.setTitle("Muh 1");
		model1.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model1);

		Car model2 = factory.get(Car.class);
		model2.setId(UUID.randomUUID());
		model2.setTitle("Muh 2");
		model2.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model2);

		Car model3 = factory.get(Car.class);
		model3.setId(UUID.randomUUID());
		model3.setTitle("Muh 3");
		model3.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model3);

		Car model4 = factory.get(Car.class);
		model4.setId(UUID.randomUUID());
		model4.setTitle("Muh 4");
		model4.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model4);

		Collection<Car> cars = carRepository.query()
				.sortDescending(Car::getTitle)
				.limit(1,2)
				.execute().collect(Collectors.toList());
		assertEquals(2, cars.size());
		assertEquals("Muhs: "+cars.stream().map(c -> c.getTitle()).collect(Collectors.joining(", ")),"Muh 3", cars.stream().findFirst().get().getTitle());
		assertEquals("Muh 2", cars.stream().skip(1).findFirst().get().getTitle());
	}

	@Test
	public void freetext() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh says the cow");
		model.setBrand(Brand.Porsche);
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Optional<Car> actualOptional = carRepository.query().freetext(Car::getTitle, "says").execute().findFirst();
		Car actual = actualOptional.orElseThrow(RuntimeException::new);
		assertEquals(model.getId(), actual.getId());
	}

	@Test
	public void withCollections() {
		WithCollections w = factory.get(WithCollections.class);
		w.setId("id");
		w.setField1(Arrays.asList("id1","id2"));
		w.setField2(new HashSet<>(Arrays.asList("field1","field2")));
		withCollectionsRepository.save(w);

		Optional<WithCollections> actualOptional = withCollectionsRepository.query().eq(WithCollections::getId, "id").execute().findFirst();
		WithCollections actual = actualOptional.orElseThrow(RuntimeException::new);
		assertEquals(w.getField1(), actual.getField1());
		assertEquals(w.getField2(), actual.getField2());
	}

	@Test
	public void withNullCollections() {
		WithCollections w = factory.get(WithCollections.class);
		w.setId("id");
		w.setField1(null);
		w.setField2(null);
		withCollectionsRepository.save(w);

		Optional<WithCollections> actualOptional = withCollectionsRepository.query().eq(WithCollections::getId, "id").execute().findFirst();
		WithCollections actual = actualOptional.orElseThrow(RuntimeException::new);
		assertNull(actual.getField1());
		assertNull(actual.getField2());
	}


	@Test
	public void deleteByQueryBuilder() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh 2");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		carRepository.query().eq(Car::getTitle, "Muh").delete();

		assertEquals(1, carRepository.query().count());
	}

	@Test
	public void deleteByqueryBuilder_subQuery() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Door door = factory.get(Door.class);
		door.setCarId(model.getId());
		door.setTitle("Door title");
		door.setId(UUID.randomUUID());
		doorRepository.save(door);


		doorRepository.query()
				.subQuery(Door::getCar
						, query -> query.eq(Car::getTitle, "Muh")
				)
				.delete();

		assertEquals(0, doorRepository.query().count());
	}

	@Test
	public void saveMultiple() {
		List<Car> models = new ArrayList<>();
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setBrand(Brand.Porsche);
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		models.add(model);
		Car model2 = factory.get(Car.class);
		model2.setId(UUID.randomUUID());
		model2.setTitle("Muh");
		model2.setBrand(Brand.Porsche);
		model2.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		models.add(model2);

		carRepository.obtainInTransaction(tx -> {
			carRepository.save(tx, models);
			return null;
		});

		List<Car> actual = carRepository.query().eq(Car::getBrand, Brand.Porsche).execute().collect(Collectors.toList());
		assertEquals(2, actual.size());
		assertEquals("Muh", actual.get(0).getTitle());
		assertEquals("Muh", actual.get(1).getTitle());

		model.setTitle("Muh2");
		model2.setTitle("Muh2");

		carRepository.obtainInTransaction(tx -> {
			carRepository.save(tx, models);
			return null;
		});

		actual = carRepository.query().eq(Car::getBrand, Brand.Porsche).execute().collect(Collectors.toList());
		assertEquals(2, actual.size());
		assertEquals("Muh2", actual.get(0).getTitle());
		assertEquals("Muh2", actual.get(1).getTitle());
	}

	@Test
	public void save_autoSaveRelations() {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		Door door1 = factory.get(Door.class);
		door1.setId(UUID.randomUUID());
		door1.setTitle("Lazy as such");
		door1.setCar(model);
		model.getDoors().add(door1);

		Door door2 = factory.get(Door.class);
		door2.setId(UUID.randomUUID());
		door2.setTitle("Lazy as well");
		door2.setCar(model);
		model.getDoors().add(door2);

		Exhaust exhaust = factory.get(Exhaust.class);
		exhaust.setBrand(Brand.Hyundai);
		exhaust.setId(UUID.randomUUID());
		model.setExhaust(exhaust);

		carRepository.save(model);

		Door actual1 = doorRepository.get(door1.getId()).orElseThrow(RuntimeException::new);
		Door actual2 = doorRepository.get(door2.getId()).orElseThrow(RuntimeException::new);
		assertEquals(door1.getId(), actual1.getId());
		assertEquals(door2.getId(), actual2.getId());
		// Since the exhaust relation is not marked as auto save, save including relations will not include it
		Optional<Exhaust> actualExhaust = exhaustRepository.get(exhaust.getId());
		assertFalse(actualExhaust.isPresent());
	}

	@Test
	public void save_autoSaveRelations_withCompoundKey() {
		Bike bike = factory.get(Bike.class);
		bike.setId(UUID.randomUUID().toString());
		bike.setBikeType(BikeType.Mountain);
		bike.setWheelSize(20);

		BikeWheel wheel = factory.get(BikeWheel.class);
		wheel.setBikeType(BikeType.Mountain);
		wheel.setSize(20);
		wheel.setColor("red");

		bike.setFrontWheel(wheel);
		bike.setBackWheel(wheel);

		bikeRepository.save(bike);

		Bike actualMountain = bikeRepository.get(bike.getId()).orElseThrow(RuntimeException::new);
		assertEquals(bike.getId(), actualMountain.getId());
		assertEquals(wheel.getBikeType(), actualMountain.getFrontWheel().getBikeType());
		assertEquals(wheel.getBikeType(), actualMountain.getBackWheel().getBikeType());
	}

	@Test
	public void save_noAutoSaveRelation() {
		Bike raceBike = factory.get(Bike.class);
		raceBike.setId(UUID.randomUUID().toString());
		raceBike.setBikeType(BikeType.Racer);
		raceBike.setWheelSize(16);

		BikeWheel auxWheel = factory.get(BikeWheel.class);
		auxWheel.setBikeType(BikeType.Racer);
		auxWheel.setSize(16);
		auxWheel.setColor("blue");

		raceBike.setAuxiliaryWheel(auxWheel);

		bikeRepository.save(raceBike);

		Bike actualRace = bikeRepository.get(raceBike.getId()).orElseThrow(RuntimeException::new);
		assertNull(actualRace.getAuxiliaryWheel());
	}

	@Test
	public void save_autoSaveRelation_viaRelation() {
		Bike bike = factory.get(Bike.class);
		bike.setId(UUID.randomUUID().toString());
		bike.setBikeType(BikeType.Mountain);
		bike.setWheelSize(20);

		BikeGear gear = factory.get(BikeGear.class);
		gear.setGearNum(8);
		bike.getGears().add(gear);

		bikeRepository.save(bike);

		Bike actual = bikeRepository.get(bike.getId()).orElseThrow(RuntimeException::new);
		assertEquals(bike.getId(), actual.getId());
		assertEquals(1, actual.getGears().size());
		assertEquals(8, actual.getGears().get(0).getGearNum());
	}

	@Test
	public void queryOtherClass() {
		Bike bike = factory.get(Bike.class);
		bike.setId(UUID.randomUUID().toString());
		bike.setBikeType(BikeType.Mountain);
		bike.setWheelSize(20);

		BikeGear gear = factory.get(BikeGear.class);
		gear.setGearNum(8);
		bike.getGears().add(gear);

		bikeRepository.save(bike);

		BikeGear actual = bikeRepository.getGear(8);
		assertEquals(8, actual.getGearNum());
	}

	@Test
	public void deleteOtherClass() {
		Bike bike = factory.get(Bike.class);
		bike.setId(UUID.randomUUID().toString());
		bike.setBikeType(BikeType.Mountain);
		bike.setWheelSize(20);

		BikeGear gear = factory.get(BikeGear.class);
		gear.setGearNum(8);
		bike.getGears().add(gear);

		bikeRepository.save(bike);

		BikeGear actual = bikeRepository.getGear(8);
		assertEquals(8, actual.getGearNum());

		bikeRepository.deleteGear(8);

		try {
			bikeRepository.getGear(8);
			fail();
		} catch (RuntimeException e) {

		}
	}

	@Test
	public void allFieldTypes() {
		AllFieldTypes obj = factory.get(AllFieldTypes.class);
		obj.setUuid(UUID.randomUUID());
		obj.setString("string");
		obj.setCharacter('c');
		obj.setZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00.000Z"));
		obj.setInstant(Instant.parse("2021-01-01T00:00:00.000Z"));
		obj.setLocalDateTime(LocalDateTime.parse("2021-01-01T00:00:00"));
		obj.setLocalDate(LocalDate.parse("2021-01-01"));
		obj.setBigDecimal(BigDecimal.valueOf(3.1415));
		obj.setAnEnum(Brand.Hyundai);

		obj.setInteger(24);
		obj.setaShort((short) 44);
		obj.setaLong(42L);
		obj.setaDouble(3.14);
		obj.setaFloat(3.15f);
		obj.setaBoolean(false);
		obj.setaByte((byte) 3);

		obj.setPrimitiveInteger(666);
		obj.setPrimitiveShort((short) 115);
		obj.setPrimitiveLong(444L);
		obj.setPrimitiveDouble(99.99);
		obj.setPrimitiveFloat(88.88f);
		obj.setPrimitiveBoolean(true);
		obj.setPrimitiveByte((byte) 9);

		allFieldTypesRepository.save(obj);

		AllFieldTypes actual = allFieldTypesRepository.get(obj.getUuid()).get();

		assertEquals(obj.getUuid(), actual.getUuid());
		assertEquals(obj.getString(), actual.getString());
		assertEquals(obj.getCharacter(), actual.getCharacter());
		assertEquals(obj.getZonedDateTime(), actual.getZonedDateTime());
		assertEquals(obj.getInstant(), actual.getInstant());
		assertEquals(obj.getLocalDateTime(), actual.getLocalDateTime());
		assertEquals(obj.getLocalDate(), actual.getLocalDate());
		assertEquals(obj.getBigDecimal().stripTrailingZeros(), actual.getBigDecimal().stripTrailingZeros());

		assertEquals(obj.getInteger(), actual.getInteger());
		assertEquals(obj.getaShort(), actual.getaShort());
		assertEquals(obj.getaLong(), actual.getaLong());
		assertEquals(obj.getaDouble(), actual.getaDouble());
		assertEquals(obj.getaFloat(), actual.getaFloat());
		assertEquals(obj.getaBoolean(), actual.getaBoolean());
		assertEquals(obj.getaByte(), actual.getaByte());

		assertEquals(obj.getPrimitiveInteger(), actual.getPrimitiveInteger());
		assertEquals(obj.getPrimitiveShort(), actual.getPrimitiveShort());
		assertEquals(obj.getPrimitiveLong(), actual.getPrimitiveLong());
		assertEquals(obj.getPrimitiveDouble(), actual.getPrimitiveDouble(),0.001);
		assertEquals(obj.getPrimitiveFloat(), actual.getPrimitiveFloat(), 0.001);
		assertEquals(obj.isPrimitiveBoolean(), actual.isPrimitiveBoolean());
		assertEquals(obj.getPrimitiveByte(), actual.getPrimitiveByte());
	}


}




