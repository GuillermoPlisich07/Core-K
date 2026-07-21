package com.konverza.shared.seed;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;
import com.konverza.shared.enums.Industry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_DEMO_USERS:true}")
    private boolean seedDemoUsersEnabled;

    @Value("${SEED_ADMIN_PASSWORD:Konverza-Admin-2026!}")
    private String seedAdminPassword;

    @Value("${SEED_EMPLOYEE_PASSWORD:Konverza-Demo-2026!}")
    private String seedEmployeePassword;

    @Value("${SEED_EXEC_PASSWORD:Konverza-Exec-2026!}")
    private String seedExecPassword;

    @Override
    public void run(String... args) {
        seedUsers();

        if (scenarioRepository.count() > 0) {
            log.info("Seed ya cargado, saltando.");
            return;
        }
        log.info("Cargando 4 escenarios de seed...");
        scenarioRepository.saveAll(buildScenarios());
        log.info("Escenarios de seed cargados correctamente.");
    }

    private void seedUsers() {
        if (!seedDemoUsersEnabled) {
            log.info("Seed de usuarios demo deshabilitado (SEED_DEMO_USERS=false).");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Usuarios de seed ya cargados, saltando.");
            return;
        }
        log.info("Cargando usuarios demo de seed (deshabilitar con SEED_DEMO_USERS=false en produccion)...");
        userRepository.saveAll(List.of(
            User.builder()
                .email("admin@konverza.com")
                .passwordHash(passwordEncoder.encode(seedAdminPassword))
                .role(User.Role.ADMIN)
                .enabled(true)
                .build(),
            User.builder()
                .email("vendedor@konverza.com")
                .passwordHash(passwordEncoder.encode(seedEmployeePassword))
                .role(User.Role.EMPLOYEE)
                .enabled(true)
                .build(),
            User.builder()
                .email("exec@konverza.com")
                .passwordHash(passwordEncoder.encode(seedExecPassword))
                .role(User.Role.EXEC)
                .enabled(true)
                .build()
        ));
        log.info("Usuarios demo de seed cargados correctamente.");
    }

    private List<Scenario> buildScenarios() {
        String defaultWeights = "{\"persuasion\":25,\"product_knowledge\":20,\"objection_handling\":25,\"fluency\":15,\"confidence\":15}";
        // avatarId stores the Tavus Persona ID configured via TAVUS_PERSONA_* env vars
        return List.of(
            Scenario.builder()
                .name("Cliente enojado - Servicio de internet")
                .description("Un cliente furioso que lleva tres dias sin internet y amenaza con cancelar el servicio.")
                .clientPersona(Scenario.ClientPersona.ANGRY)
                .difficulty(Scenario.Difficulty.HARD)
                .industries(Set.of(Industry.TELCO))
                .maxDurationMinutes(30)
                .systemPrompt("Sos Carlos Mendez, un cliente furioso de 45 anos. Llevas TRES DIAS sin internet y ya llamaste 5 veces al soporte sin solucion. Tu negocio depende del internet y perdiste plata por esto. Estas al borde de cancelar el contrato. Hablas en espanol rioplatense. Sos agresivo pero no grosero. Interrumpes al vendedor cuando da excusas. Si el vendedor te ofrece una solucion concreta (tecnico hoy, descuento real), bajas un poco la guardia. Nunca aceptes la primera oferta. Exigi compensacion por los dias sin servicio.")
                .objectionsGuide("{\"objections\": [\"Tres dias sin internet y nadie me llamo\", \"El soporte tecnico es una porqueria\", \"Me voy a otra empresa\"]}")
                .faq("{\"preguntas\": [\"Cuando me arreglan el servicio?\", \"Tienen algun descuento por la interrupcion?\"]}")
                .avatarId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"lo que pasa es que\",\"entiendo su frustraciÃ³n pero\",\"no es culpa nuestra\",\"eso no depende de mÃ­\",\"el sistema no me deja\",\"hable con otro departamento\"]")
                .vendedorRol("Agente de Soporte y RetenciÃ³n")
                .escenarioObjetivo("Retener al cliente, ofrecer soluciÃ³n concreta y compensaciÃ³n por el corte de servicio")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente indiferente - Software de gestion")
                .description("Un dueno de empresa que no ve la necesidad de digitalizar sus procesos.")
                .clientPersona(Scenario.ClientPersona.INDIFFERENT)
                .difficulty(Scenario.Difficulty.MEDIUM)
                .industries(Set.of(Industry.SOFTWARE_B2B))
                .maxDurationMinutes(30)
                .systemPrompt("Sos Roberto Paz, dueno de una ferreteria con 15 empleados. Tenes 58 anos y no sos muy tecnologico. Tu negocio funciona bien con planillas y papel. No ves para que cambiar lo que funciona. Hablas en espanol rioplatense. Sos educado pero desinteresado. Respondes con monosilabos si no te convencen. Si el vendedor te muestra un beneficio muy concreto en pesos (ahorro de tiempo = plata), prestas mas atencion. Tu mayor miedo es que el sistema falle justo en epoca de inventario.")
                .objectionsGuide("{\"objections\": [\"Nosotros con planillas de Excel nos manejamos re bien\", \"No tengo tiempo para aprender software nuevo\", \"Es muy caro para lo que ofrece\"]}")
                .faq("{\"preguntas\": [\"Tiene integracion con AFIP?\", \"Puedo migrar mis datos de Excel?\"]}")
                .avatarId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"todo el mundo lo usa\",\"es muy fÃ¡cil\",\"en cinco minutos lo aprendÃ©s\",\"esto lo maneja hasta un nene\",\"no tiene pÃ©rdida\",\"no hay nada que aprender\"]")
                .vendedorRol("SDR â€” Sales Development Representative")
                .escenarioObjetivo("Generar interÃ©s inicial y agendar una demo gratuita de 30 dÃ­as del ERP")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente exigente - Consultoria de RRHH")
                .description("Directora de RRHH de una empresa mediana que exige resultados medibles y ROI claro.")
                .clientPersona(Scenario.ClientPersona.DEMANDING)
                .difficulty(Scenario.Difficulty.HARD)
                .industries(Set.of(Industry.CONSULTORIA))
                .maxDurationMinutes(45)
                .systemPrompt("Sos Valeria Torres, Directora de RRHH de una empresa de 200 personas. Sos MBA, muy analitica y exigente. Hablas en espanol rioplatense. Haces preguntas dificiles sobre metodologia, tiempos y metricas. Esperas respuestas concretas con numeros. Si el vendedor es vago, lo presionas mas. Tuviste una mala experiencia con una consultora anterior que prometio mucho y entrego poco. Solo cerras si el vendedor puede darte casos de exito verificables y un cronograma detallado.")
                .objectionsGuide("{\"objections\": [\"Que ROI concreto me garantizan?\", \"Necesito metricas claras antes de contratar\", \"Tuvimos una consultora antes y fue un fracaso\"]}")
                .faq("{\"preguntas\": [\"Cuanto tiempo lleva el proceso de seleccion?\", \"Tienen experiencia en mi industria?\"]}")
                .avatarId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights("{\"persuasion\":20,\"product_knowledge\":25,\"objection_handling\":30,\"fluency\":10,\"confidence\":15}")
                .forbiddenPhrases("[\"confÃ­e en nosotros\",\"somos los mejores del mercado\",\"todos nuestros clientes estÃ¡n felices\",\"eso lo podemos ver despuÃ©s\",\"no se preocupe por eso\",\"es un estÃ¡ndar de la industria\"]")
                .vendedorRol("Account Executive")
                .escenarioObjetivo("Cerrar contrato de consultorÃ­a presentando casos verificables y cronograma detallado")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente dificil - Producto financiero")
                .description("Un inversor esceptico que desconfia de los productos financieros luego de malas experiencias.")
                .clientPersona(Scenario.ClientPersona.DIFFICULT)
                .difficulty(Scenario.Difficulty.MEDIUM)
                .industries(Set.of(Industry.FINANZAS))
                .maxDurationMinutes(30)
                .systemPrompt("Sos Marcelo Gimenez, inversor particular de 52 anos. Perdiste dinero en 2001 y con una financiera en 2019. Desconfias de todos los productos financieros. Hablas en espanol rioplatense. Haces preguntas tecnicas sobre regulacion, garantias y riesgo. Buscas contradicciones en lo que dice el vendedor. Si el vendedor menciona la CNV, BCRA o garantias concretas, bajas un poco la guardia. Necesitas sentir que el vendedor entiende tus miedos antes de considerar invertir.")
                .objectionsGuide("{\"objections\": [\"Ya me estafaron con un fondo parecido\", \"Esta regulado por la CNV?\", \"El plazo fijo me da mas tranquilidad\"]}")
                .faq("{\"preguntas\": [\"Que pasa si necesito el dinero antes?\", \"Esta garantizado el capital?\"]}")
                .avatarId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"garantizado al 100%\",\"no hay riesgo\",\"imposible perder\",\"todos invierten en esto\",\"el gobierno lo respalda\",\"yo tambiÃ©n invierto aquÃ­\"]")
                .vendedorRol("Asesor de Inversiones / Wealth Manager")
                .escenarioObjetivo("Convencer al inversor escÃ©ptico para colocar un mÃ­nimo de $100.000 en el fondo de renta fija")
                .createdBy("MANUAL")
                .build()
        );
    }
}
