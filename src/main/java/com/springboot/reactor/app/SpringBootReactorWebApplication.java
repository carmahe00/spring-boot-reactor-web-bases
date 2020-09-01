package com.springboot.reactor.app;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.springboot.reactor.app.models.Comentarios;
import com.springboot.reactor.app.models.Usuario;
import com.springboot.reactor.app.models.UsuarioComentarios;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringBootReactorWebApplication implements CommandLineRunner{

	private static final Logger log  = LoggerFactory.getLogger(SpringBootReactorWebApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootReactorWebApplication.class, args);
	}

	
	@Override
	public void run(String... args) throws Exception {
		ejemploContraPresion();
	}
	
	/**
	 * método para manejar la contrapresión (la carga de datos)
	 * 
	 * .log() Permite ver la traza completa
	 * Subscriber permite manejar el flujo
	 */
	public void ejemploContraPresion() {
		Flux.range(1, 10)
		.log()
		//.limitRate(5)
		.subscribe(
			new Subscriber<Integer>() {

			/**
			 * @param s es subscriber lo usamos para hacer las solicitudes (request)
			 */
			private Subscription s;
			
			/**
			 * @param limite indica el limite de la solicitud
			 */
			private Integer limite = 5;
			/**
			 * @param consumido contador para llegar al limite
			 */
			private Integer consumido = 0;
			
			/**
			 * método de subscripción
			 * 
			 * @param s es el subscriber que contiene el flujo
			 */
			@Override
			public void onSubscribe(Subscription s) {
				this.s =s;
				s.request(limite);
			}

			/**
			 * método que obtiene los datos
			 * 
			 * @param t dato del flujo
			 */
			@Override
			public void onNext(Integer t) {
				log.info(t.toString());
				consumido++;
				//Si llega al limite vuelve hacer la petición
				if(consumido ==  limite) {
					consumido =0;
					s.request(limite);
				}
			}

			@Override
			public void onError(Throwable t) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	/**
	 * método para crear un Observable desde cero
	 */
	public void ejemploIntervalDesdeCreate() {
		Flux.create(emitter -> {
			Timer timer = new Timer();
			//Agenda Tarea
			timer.schedule(new TimerTask() {
				private Integer contador = 0;
				@Override
				public void run() {
					emitter.next(++contador);
					//Romper el ciclo infinito
					if(contador ==10) {
						timer.cancel();
						emitter.complete();
					}
					if(contador == 5) {
						timer.cancel();
						emitter.error(new InterruptedException("Error, se ha detenido el flux en 5!"));
					}
					
				}
			}, 1000, 1000);
		})
		.subscribe(
				//Función
				next -> log.info(next.toString()),
				//Error
				error -> log.info(error.getMessage()),
				//Completado el flujo
				()->log.info("Hemos terminado"));
		
	}
	
	/**
	 * Intervalo infinito con interval 
	 * @throws InterruptedException expeción de  CountDownLatch
	 * .rety() intenta n veces
	 * 
	 */
	public void ejemploIntervalInfinito() throws InterruptedException {
		
		//Contador incicia en 1
		CountDownLatch latch = new CountDownLatch(1);
		
		Flux.interval(Duration.ofSeconds(1))
		.doOnTerminate(latch::countDown)
		//Convierte Flux de Long a Flux de error 
		.flatMap(i -> {
			if(i >= 5) {
				return Flux.error(new InterruptedException("Solo hasta 5"));
			}
			return Flux.just(i); 
		})
		.map(i -> "Hola "+i)
		//Intenta dos verces
		.retry(2)
		//subscribe()
		.subscribe(s -> log.info(s), e -> log.error(e.getMessage()));
		
		latch.await();
	}
	
	/**
	 * método con operador de tiempo com delayElement()
	 * .delayElements() retrasa cada uno de estos elementos de Flux
	 */
	public void ejemploDelayElements() {
		Flux<Integer> rango = Flux.range(1, 12)
				.delayElements(Duration.ofSeconds(1))
				.doOnNext(i -> log.info(i.toString()));
		rango.blockLast();
	}
	
	/**
	 * método con operador de tiempo interval y zipWith
	 * .blockLast() funciona igual .subscribe(), pero bloquea hasta que ejecute el último elemento
	 */
	public void ejemploInterval() {
		Flux<Integer> rango = Flux.range(1, 12);
		Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));
		
		rango.zipWith(retraso, (ra, re) -> re)
		.doOnNext(i-> log.info(i.toString()))
		.blockLast();
	}
	
	/**
	 * método operador Range
	 * 
	 * .zipWith() combina elementos el flujos de números y el flujo de rangos
	 */
	public void ejemploZipWithRangos() {
		Flux<Integer> rangos = Flux.range(0, 4);
		Flux.just(1,2,3,4)
		.map(i -> (i*2))
		.zipWith(rangos, (numeros,rango) -> String.format("Primer Flux: %d, Segundo Flux: %d", numeros, rango))
		.subscribe(texto -> log.info(texto));
	}
	
	/**
	 * Método para convinar flujos con el operador flatMap
	 * 
	 * Mono crear un flujo de Usuario .fromCallable() funciona igual que just 
	 */
	public void ejemploUsuarioComentarioFlatMap() {
		Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Jhon", "Doe"));
		
		Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
			Comentarios comentarios = new Comentarios();
			comentarios.addComentarios("Hola pepe, qué tal!");
			comentarios.addComentarios("Mañana voy a la playa!");
			comentarios.addComentarios("Estoy tomando curos de spring con reactor!");
			return comentarios;
		});
		
		//Convinar dos flujos
		usuarioMono.flatMap(u -> comentariosMono.map(c -> new UsuarioComentarios(u, c)))
			.subscribe(uc -> log.info(uc.toString()));
	}
	
	/**
	 * método convertir Observable Flux a Mono
	 */
	public void ejemploCollectList() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andrés", "Guzman"));
		usuariosList.add(new Usuario("Pedro", "Fulano"));
		usuariosList.add(new Usuario("Maria", "Fulana"));
		usuariosList.add(new Usuario("Diego", "Sultano"));
		usuariosList.add(new Usuario("Juan", "Mengano"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));
		
		Flux.fromIterable(usuariosList)
			.collectList()
			.subscribe(	lista -> {
				lista.forEach(item -> log.info(item.toString()));
			});
		
	}
	
	/**
	 * método convertir Flux List a Flux String
	 * @throws Exception
	 */
	public void ejemploToString() throws Exception {
		List<Usuario> usuariosList = new ArrayList<>();
		usuariosList.add(new Usuario("Andrés", "Guzman"));
		usuariosList.add(new Usuario("Pedro", "Fulano"));
		usuariosList.add(new Usuario("Maria", "Fulana"));
		usuariosList.add(new Usuario("Diego", "Sultano"));
		usuariosList.add(new Usuario("Juan", "Mengano"));
		usuariosList.add(new Usuario("Bruce", "Lee"));
		usuariosList.add(new Usuario("Bruce", "Willis"));
		
		Flux.fromIterable(usuariosList)
				.map(usuario->  usuario.getNombre().toUpperCase().concat(" ").concat(usuario.getApellido().toUpperCase()))
				.flatMap(nombre -> {
					if (nombre.contains("bruce".toUpperCase())) {
						return Mono.just(nombre);
					}else {
						return Mono.empty();
					}
					
				})
				.map(nombre ->  {
					return nombre.toLowerCase();
				}).subscribe(u -> log.info(u.toString()));
		
	}
	
	/**
	 * 
	 * .flatMap() convierte un observable de tipo flux a Mono 
	 */
	public void ejemploFlatMap() throws Exception {
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andrés Guzman");
		usuariosList.add("Diego Fulano");
		usuariosList.add("Maria Fulana");
		usuariosList.add("Diego Sultano");
		usuariosList.add("Juan Mengano");
		usuariosList.add("Bruce Martín");
		usuariosList.add("Bruce Jeam");
		
		Flux.fromIterable(usuariosList).map(nombre ->  new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				.flatMap(usuario -> {
					if (usuario.getNombre().equalsIgnoreCase("bruce")) {
						return Mono.just(usuario);
					}else {
						return Mono.empty();
					}
					
				})
				.map(usuario ->  {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				}).subscribe(u -> log.info(u.toString()));
		
	}
	
	/**
	 * .just() elemento del flujo
	 * .map() transforma lso datos
	 * .filter() filtra los datos
	 * .doOnNext() notifica que ha llegado un elemento
	 */
	public void ejemploIterable() throws Exception {
		List<String> usuariosList = new ArrayList<>();
		usuariosList.add("Andrés Guzman");
		usuariosList.add("Diego Fulano");
		usuariosList.add("Maria Fulana");
		usuariosList.add("Diego Sultano");
		usuariosList.add("Juan Mengano");
		usuariosList.add("Bruce Martín");
		usuariosList.add("Bruce Jeam");
		
		Flux<String> nombres = Flux.fromIterable(usuariosList);//Flux.just("Andrés Guzman", "Diego Fulano", "Pedro Mengano","Maria Carvajal","Juan Arco", "Bruce Martín", "Bruce Jeam");
		
				Flux<Usuario> usuarios = nombres.map(nombre ->  new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
				.filter(usuario -> usuario.getNombre().toLowerCase().equals("bruce"))
				.doOnNext( usuario ->{
					if(usuario == null) {
						throw new RuntimeException("Nombre no puede ser vacío");
					}
					System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));
					
				})
				.map(usuario ->  {
					String nombre = usuario.getNombre().toLowerCase();
					usuario.setNombre(nombre);
					return usuario;
				});
		
		usuarios.subscribe(e -> log.info(e.toString()),
				error -> log.error(error.getMessage()),
				new Runnable() {
					
					@Override
					public void run() {
						log.info("Ha finalizado la ejecución desde el observable con éxito!");
						
					}
				});
		
	}
	

}
