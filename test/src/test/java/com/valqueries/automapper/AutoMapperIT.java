package com.valqueries.automapper;

import com.google.inject.Guice;
import com.valqueries.Database;
import com.valqueries.IOrm;
import io.ran.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class AutoMapperIT extends AutoMapperBaseTests {


	abstract Database database();

	@Override
	protected void setInjector() {
		database = database();
		GuiceModule module = new GuiceModule(database, ValqueriesResolver.class);
		injector = Guice.createInjector(module);
		factory = injector.getInstance(GenericFactory.class);
	}

	@Before
	public void setup() throws Throwable {
		sqlGenerator = injector.getInstance(SqlGenerator.class);
		TestClasses testClasses = getClass().getMethod(name.getMethodName()).getAnnotation(TestClasses.class);

		List<Class> clazzes = Arrays.asList(Car.class, Door.class, Engine.class, EngineCar.class, Exhaust.class, Tire.class, WithCollections.class, Bike.class, BikeGear.class, BikeGearBike.class, BikeWheel.class, PrimaryKeyModel.class, Bipod.class, Pod.class, AllFieldTypes.class);
		if (testClasses.value() != null) {
			clazzes = Arrays.asList(testClasses.value());
		}
		try (IOrm orm = database.getOrm()) {
			clazzes.forEach(c -> {
				TypeDescriber desc = TypeDescriberImpl.getTypeDescriber(c);
				try {
					orm.update("DROP TABLE " + sqlGenerator.getTableName(desc) + ";");
				} catch (Exception e) {

				}
				sqlGenerator.generateCreateTable(desc);
			});
		}
	}

	@After
	public void cleanup() {

	}

	@Test
	@TestClasses(Car.class)
	public void eagerLoad() throws Throwable {
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

		Collection<Car> cars = carRepository.getAllEager();
		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);
//		when(resolver.getCollection(any(),anyString(),any())).thenReturn(Collections.emptyList());

		assertEquals(1, cars.size());
		List<Door> doors = cars.stream().findFirst().get().getDoors();
		assertEquals(2, doors.size());

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Exhaust.class, Door.class})
	public void eagerLoad_multiple() throws Throwable {
		Exhaust exhaust = factory.get(Exhaust.class);
		exhaust.setId(UUID.randomUUID());
		exhaust.setBrand(Brand.Porsche);
		CrudRepository.CrudUpdateResult updresult = exhaustRepository.save(exhaust);

		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setExhaust(exhaust);
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

		Collection<Car> cars = carRepository.query().withEager(Car::getDoors).withEager(Car::getExhaust).execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		assertEquals(1, cars.size());
		List<Door> doors = cars.stream().findFirst().get().getDoors();
		assertEquals(2, doors.size());
		Exhaust actualExhaust = cars.stream().findFirst().get().getExhaust();
		assertEquals(exhaust.getId(), actualExhaust.getId());

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Door.class})
	public void eagerLoad_noMoreCollectionInteractions() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.withEager(Car::getDoors)
				.execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		assertEquals(1, cars.size());
		List<Door> doors = cars.stream().findFirst().get().getDoors();

		assertEquals(0, doors.size());

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Exhaust.class})
	public void eagerLoad_noMoreObjectInteraction() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.withEager(Car::getExhaust)
				.execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		Exhaust actualExhaust = cars.stream().findFirst().get().getExhaust();
		assertNull(actualExhaust);

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Exhaust.class, Door.class})
	public void eagerLoad_multiple_noInteractions() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.withEager(Car::getDoors)
				.withEager(Car::getExhaust).execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		assertEquals(1, cars.size());
		List<Door> doors = cars.stream().findFirst().get().getDoors();

		assertEquals(0, doors.size());

		Exhaust actualExhaust = cars.stream().findFirst().get().getExhaust();
		assertNull(actualExhaust);

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Tire.class})
	public void eagerLoad_fromCompoundKey() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Tire tire = factory.get(Tire.class);
		tire.setCar(model);
		tire.setBrand(Brand.Porsche);
		tireRepository.save(tire);

		Tire res = tireRepository.query()
				.eq(Tire::getCarId, model.getId())
				.withEager(Tire::getCar)
				.execute().findFirst().orElseThrow(() -> new RuntimeException());

		res.getClass().getMethod("_resolverInject", Resolver.class).invoke(res, resolver);

		Car actual = res.getCar();
		assertNotNull(actual);

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Bike.class, BikeWheel.class})
	public void eagerLoad_multipleRelationFields() throws Throwable {
		Bike bike = factory.get(Bike.class);
		bike.setId(UUID.randomUUID().toString());
		bike.setBikeType(BikeType.Mountain);
		bike.setWheelSize(20);

		BikeWheel wheel = factory.get(BikeWheel.class);
		wheel.setBikeType(BikeType.Mountain);
		wheel.setSize(20);
		wheel.setColor("red");

		bike.setFrontWheel(wheel);

		bikeRepository.save(bike);

		Bike res = bikeRepository.query()
				.eq(Bike::getId, bike.getId())
				.withEager(Bike::getFrontWheel)
				.execute().findFirst().orElseThrow(() -> new RuntimeException());

		res.getClass().getMethod("_resolverInject", Resolver.class).invoke(res, resolver);

		assertNotNull(res);

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Door.class})
	public void lazyLoad_emptyCollection_onlyOneInteractionWithResolver() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		Car car = cars.stream().findFirst().get();
		List<Door> actualDoors = car.getDoors();
		assertEquals(0, actualDoors.size());
		verify(resolver, times(1)).getCollection(eq(Car.class), eq("doors"), eq(car));

		actualDoors = car.getDoors();
		assertEquals(0, actualDoors.size());
		verifyNoMoreInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Exhaust.class, Door.class})
	public void lazyLoad_nullObject_onlyOneInteractionWithResolver() throws Throwable {
		Car model = factory.get(Car.class);
		model.setId(UUID.randomUUID());
		model.setTitle("Muh");
		model.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
		carRepository.save(model);

		Collection<Car> cars = carRepository.query()
				.execute().collect(Collectors.toList());

		Class<? extends Car> cl = cars.stream().findFirst().get().getClass();
		cl.getMethod("_resolverInject", Resolver.class).invoke(cars.stream().findFirst().get(), resolver);

		Car car = cars.stream().findFirst().get();
		Exhaust actualExhaust = car.getExhaust();
		assertNull(actualExhaust);
		verify(resolver, times(1)).get(eq(Car.class), eq("exhaust"), eq(car));

		actualExhaust = car.getExhaust();
		assertNull(actualExhaust);
		verifyNoMoreInteractions(resolver);
	}

	@Test
	@TestClasses({PrimaryKeyModel.class})
	public void primaryKeyOnlyModel_savedMultipleTimes() throws Throwable {
		PrimaryKeyModel model = factory.get(PrimaryKeyModel.class);
		model.setFirst("1");
		model.setSecond("2");
		primayKeyModelRepository.save(model);
		primayKeyModelRepository.save(model);
	}

	@Test
	@TestClasses({PrimaryKeyModel.class})
	public void primaryKeyOnlyModelList_savedMultipleTimes() throws Throwable {
		PrimaryKeyModel model = factory.get(PrimaryKeyModel.class);
		model.setFirst("1");
		model.setSecond("2");
		PrimaryKeyModel model2 = factory.get(PrimaryKeyModel.class);
		model2.setFirst("2");
		model2.setSecond("3");
		primayKeyModelRepository.doRetryableInTransaction(tx -> {
			primayKeyModelRepository.save(tx, Arrays.asList(model, model2));
		});
		primayKeyModelRepository.doRetryableInTransaction(tx -> {
			primayKeyModelRepository.save(tx, Arrays.asList(model, model2));
		});
	}

	@Test
	@TestClasses({Pod.class, Bipod.class})
	public void twoRelationsToSameClassOnOneObject() throws Throwable {
		Pod pod1 = factory.get(Pod.class);
		pod1.setId("pod1");
		pod1.setName("Pod number 1");

		Pod pod2 = factory.get(Pod.class);
		pod2.setId("pod2");
		pod2.setName("Pod number 2");

		Bipod bipod = factory.get(Bipod.class);
		bipod.setId(UUID.randomUUID().toString());
		bipod.setPod1(pod1);
		bipod.setPod2(pod2);

		podRepository.save(bipod);


		Bipod actual = podRepository.getEagerBipod(bipod.getId());
		actual.getClass().getMethod("_resolverInject", Resolver.class).invoke(actual, resolver);

		assertEquals(bipod.getId(), actual.getId());
		assertEquals("Pod number 1", actual.getPod1().getName());
		assertEquals("Pod number 2", actual.getPod2().getName());

		verifyNoInteractions(resolver);
	}

	@Test
	@TestClasses({Car.class, Driver.class, DriverCar.class, Door.class})
	public void save_manyToMany_eagerWithMoreJoins() throws Throwable {
		Car nissan = factory.get(Car.class);
		nissan.setId(UUID.randomUUID());
		nissan.setTitle("Nissan");
		nissan.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Car citroen = factory.get(Car.class);
		citroen.setId(UUID.randomUUID());
		citroen.setTitle("Citroen");
		citroen.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Car tesla = factory.get(Car.class);
		tesla.setId(UUID.randomUUID());
		tesla.setTitle("Tesla");

		Driver pilot1 = factory.get(Driver.class);
		pilot1.setId("pilot1");
		pilot1.setName("pedrito");

		Driver pilot2 = factory.get(Driver.class);
		pilot2.setId("pilot2");
		pilot2.setName("jorgito");

		Door nissanDoor = factory.get(Door.class);
		nissanDoor.setId(UUID.randomUUID());
		nissanDoor.setTitle("Nissan door");
		nissanDoor.setCarId(nissan.getId());

		nissan.setDoors(Collections.singletonList(nissanDoor));
		nissan.setDrivers(Arrays.asList(pilot1, pilot2));
		citroen.setDrivers(Arrays.asList(pilot1, pilot2));

		carRepository.save(nissan);
		carRepository.save(citroen);
		carRepository.save(tesla);

		Collection<Car> cars = carRepository.query()
				.withEager(Car::getDrivers)
				.withEager(Car::getDoors)
				.execute().collect(Collectors.toList());

		assertTrue(cars.stream().filter(car -> !car.getTitle().equals("Tesla")).allMatch(car -> car.drivers.size() == 2));
		assertTrue(cars.stream().filter(car -> car.getTitle().equals("Tesla")).allMatch(car -> car.drivers.isEmpty())); //autopilot
		assertEquals(1, cars.stream().filter(car -> car.getTitle().equals("Nissan")).findFirst().get().getDoors().size());
		assertEquals(0, cars.stream().filter(car -> car.getTitle().equals("Citroen")).findFirst().get().getDoors().size());
	}

	@Test
	@TestClasses({Car.class, Driver.class, DriverCar.class})
	@Ignore
	public void save_manyToMany_eagerWithSubquery_doesNotFilterResults() throws Throwable {
		Car nissan = factory.get(Car.class);
		nissan.setId(UUID.randomUUID());
		nissan.setTitle("Nissan");
		nissan.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Car citroen = factory.get(Car.class);
		citroen.setId(UUID.randomUUID());
		citroen.setTitle("Citroen");
		citroen.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Driver pilot1 = factory.get(Driver.class);
		pilot1.setId("pilot1");
		pilot1.setName("pedrito");

		Driver pilot2 = factory.get(Driver.class);
		pilot2.setId("pilot2");
		pilot2.setName("jorgito");

		Driver pilot3 = factory.get(Driver.class);
		pilot3.setId("pilot3");
		pilot3.setName("juancito");

		nissan.setDrivers(Arrays.asList(pilot1, pilot2));
		citroen.setDrivers(Arrays.asList(pilot1, pilot3));

		carRepository.save(nissan);
		carRepository.save(citroen);

		Collection<Car> cars = carRepository.query()
				.subQueryList(Car::getDrivers, sq -> sq.eq(Driver::getName, "jorgito"))
				.withEager(Car::getDrivers)
				.execute().collect(Collectors.toList());

		Car car = cars.stream().findAny().get();
		assertEquals(1, cars.size());
		assertEquals(2, car.drivers.size());
		assertTrue(car.drivers
				.stream()
				.allMatch(driver -> driver.getName().equals("jorgito")
						|| driver.getName().equals("pedrito")));
	}

	@Test
	@TestClasses({Car.class, Driver.class, DriverCar.class})
	@Ignore
	public void save_manyToMany_lazyWithSubquery() throws Throwable {
		Car nissan = factory.get(Car.class);
		nissan.setId(UUID.randomUUID());
		nissan.setTitle("Nissan");
		nissan.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Car citroen = factory.get(Car.class);
		citroen.setId(UUID.randomUUID());
		citroen.setTitle("Citroen");
		citroen.setCreatedAt(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));

		Driver pilot1 = factory.get(Driver.class);
		pilot1.setId("pilot1");
		pilot1.setName("pedrito");

		Driver pilot2 = factory.get(Driver.class);
		pilot2.setId("pilot2");
		pilot2.setName("jorgito");

		Driver pilot3 = factory.get(Driver.class);
		pilot3.setId("pilot3");
		pilot3.setName("juancito");

		nissan.setDrivers(Arrays.asList(pilot1, pilot2));
		citroen.setDrivers(Arrays.asList(pilot1, pilot3));

		carRepository.save(nissan);
		carRepository.save(citroen);

		Collection<Car> cars = carRepository.query()
				.subQueryList(Car::getDrivers, sq -> sq.eq(Driver::getName, "jorgito"))
				.execute().collect(Collectors.toList());

		assertEquals(1, cars.size());
		Car car = cars.stream().findAny().get();
		assertNull(car.drivers);
	}
}
