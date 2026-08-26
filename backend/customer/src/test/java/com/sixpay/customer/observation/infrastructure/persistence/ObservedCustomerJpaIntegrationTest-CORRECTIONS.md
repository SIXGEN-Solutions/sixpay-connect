# Corrections obligatoires dans ObservedCustomerJpaIntegrationTest

## 1. Ajouter cet import

```java
import com.sixpay.customer.configuration.CustomerModuleConfiguration;
import org.springframework.boot.autoconfigure.data.jpa
        .JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config
        .EnableJpaRepositories;
```

## 2. Remplacer l'annotation SpringBootTest par

```java
@SpringBootTest(
        classes =
                ObservedCustomerJpaIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "sixpay.customer.observation.persistence.enabled=true",
                "sixpay.customer.observation.audit.persistence.enabled=false",
                "sixpay.customer.observation.query.enabled=false",
                "sixpay.customer.verification.banking.enabled=false",
                "sixpay.customer.observation.persistence."
                        + "max-optimistic-attempts=3",
                "sixpay.customer.observation.persistence."
                        + "protection-key-base64="
                        + "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        }
)
```

## 3. Supprimer de DynamicPropertySource

```java
sixpay.customer.observation.persistence.enabled
sixpay.customer.observation.persistence.protection-key-base64
sixpay.customer.observation.persistence.max-optimistic-attempts
```

Ces propriétés sont maintenant forcées par `@SpringBootTest`.

## 4. Remplacer TestApplication par

```java
@SpringBootConfiguration
@EnableAutoConfiguration(
        exclude = {
                JpaRepositoriesAutoConfiguration.class,
                CustomerModuleConfiguration.class
        }
)
@EntityScan(
        basePackages = {
                "com.sixpay.customer.observation."
                        + "infrastructure.persistence.entity"
        }
)
@EnableJpaRepositories(
        basePackages = {
                "com.sixpay.customer.observation."
                        + "infrastructure.persistence.repository"
        }
)
@Import({
        ObservedCustomerPersistenceConfiguration.class,
        IntegrationTestConfiguration.class
})
static class TestApplication {
}
```

Cette isolation empêche :
- le chargement automatique de `CustomerModuleConfiguration`;
- la découverte du repository d'audit;
- le double scan des repositories JPA;
- l'activation involontaire de la query API.
