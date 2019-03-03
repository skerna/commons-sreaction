# SKERNA SREACTION

Sreaction is not more than, wrapper of result value, which allows the client to obtain any throwable exception or result
after invoking a function

never use this on something that returns something like Option<Option<T>>
instead, use flat result

## Motivación

En clean architecture, Hexagonal architecture, el dominio
de negocio es modelado como casos de uso (service method).

la unica información que escapa de estos metodos son los (domain events)
pero que pasa con las Exceptions no controladas? problemas etc.

permita planetear estas premisas:

- Las exceptions deberían tratarse tan pronto como se producen
- No seria bueno llenar de try catch la llamada a un metodo inseguro, se pirde
legibilidad del código. 

¿Que pasa en la programación asíncrona?
Las exceptions deben tratarse lo mas pronto posible y es por esto que enlugar de pasar callbacks 
como parametros (CallbackHell)se retorna un sfutere que puede contener o no datos y errores.

Estos dos nuevos tipos de datos, ademas pueden servir para mejorar la legibilidad del código:

- Centralizar el manejo de errores
- Proporcionar NullSafe
operacion que puede terminar con una exception disparada

Este mismo codigo es capaz de ejecutar en Browser, NODEJS, JVM incluso en Native code

```Kotlin
    // Reaction espera un tipo Int
    // Simplificado
    // No se puede dividir algo para zero, Exception esperada
    val result:Int? = doMathDivOperation(2,0)
                           .handleExceptionsWith(GlobalErrorHandler)// Logs, influxdb etc...
                           .result()
  
    if(result != null){
        println("El resultado es: ${result}" )            
    }
    
```
El mismo ejemmplo anterior usando inline functions. 
```kotlin
    /// Caso Inline
    // No se puede dividir algo para zero, Exception esperada
    val result:Int  = Reaction.react { 2 / 0 } 
                                    .handleExceptionsWith(GlobalErrorHandler)// Logs, influxdb etc...
                                    .result()?:0 // Elvis null safe
    
    println("El resultado es: ${result}" )            
    
```


#### Modo async

En lugar de manejar directamente los errores simplemente establece un handler async result 

```kotlin
    reaction.handle { it ->
        println("Esto termino con estaado ${it.succeded()}")
    }

```


####Integracion con Kotlin coroutines en Dev stage 

### Enfoque
Algo parecido a lo que brinda Golang para el manejo de errores
retornando el error o el object 

```go
    f, err := os.Open("filename.ext")
    if err != nil {
        log.Fatal(err)
    }
    // do something with the open *File f
```


### En kotlin la respuesta seria similar a lo siguiente:

#### Ejemplo caso de uso "Save configs users"

```kotlin
    override fun saveSettings(command: SaveSettingsCmd) = Reaction.react{
          logger.debug("Register new applicatio using context $command")
          ........
          ........
          userSettingsRepository.save(appSetting)
    }
```

¿La operacion se completo de forma correcta?

```kotlin
    val reactionSave:Settings = serviceSettings.saveSettings(someObjectSettings)
    if(reactionSave.failed()){
        // notify
    }
    // more code

```

# Maps 

```kotlin
    val reactionSave:Number = serviceSettings.saveSettings(someObjectSettings).map({
        it.someProp * 2
    })

```
