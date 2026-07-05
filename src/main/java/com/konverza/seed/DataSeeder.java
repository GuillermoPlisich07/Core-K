package com.konverza.seed;

import com.konverza.entity.Scenario;
import com.konverza.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ScenarioRepository scenarioRepository;

    @Override
    public void run(String... args) {
        if (scenarioRepository.count() > 0) {
            log.info("Seed ya cargado, saltando.");
            return;
        }
        log.info("Cargando 4 escenarios de seed...");
        scenarioRepository.saveAll(buildScenarios());
        log.info("Escenarios de seed cargados correctamente.");
    }

    private List<Scenario> buildScenarios() {
        String defaultWeights = "{\"persuasion\":25,\"product_knowledge\":20,\"objection_handling\":25,\"fluency\":15,\"confidence\":15}";
        // avatarVoiceId stores the Tavus Persona ID configured via TAVUS_PERSONA_* env vars
        return List.of(
            Scenario.builder()
                .name("Cliente enojado - Servicio de internet")
                .description("Un cliente furioso que lleva tres dias sin internet y amenaza con cancelar el servicio.")
                .clientPersona(Scenario.ClientPersona.ANGRY)
                .difficulty(Scenario.Difficulty.HARD)
                .industry(Scenario.Industry.TELCO)
                .maxDurationMinutes(30)
                .productContext("Servicio de internet residencial 300 Mbps, $1500/mes. SLA de 4hs de respuesta.")
                .systemPrompt("Sos Carlos Mendez, un cliente furioso de 45 anos. Llevas TRES DIAS sin internet y ya llamaste 5 veces al soporte sin solucion. Tu negocio depende del internet y perdiste plata por esto. Estas al borde de cancelar el contrato. Hablas en espanol rioplatense. Sos agresivo pero no grosero. Interrumpes al vendedor cuando da excusas. Si el vendedor te ofrece una solucion concreta (tecnico hoy, descuento real), bajas un poco la guardia. Nunca aceptes la primera oferta. Exigi compensacion por los dias sin servicio.")
                .objectionsGuide("{\"objections\": [\"Tres dias sin internet y nadie me llamo\", \"El soporte tecnico es una porqueria\", \"Me voy a otra empresa\"]}")
                .paymentInfo("Factura mensual, vencimiento dia 10. Multa por cancelacion anticipada: 2 meses.")
                .faq("{\"preguntas\": [\"Cuando me arreglan el servicio?\", \"Tienen algun descuento por la interrupcion?\"]}")
                .avatarVoiceId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"lo que pasa es que\",\"entiendo su frustración pero\",\"no es culpa nuestra\",\"eso no depende de mí\",\"el sistema no me deja\",\"hable con otro departamento\"]")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente indiferente - Software de gestion")
                .description("Un dueno de empresa que no ve la necesidad de digitalizar sus procesos.")
                .clientPersona(Scenario.ClientPersona.INDIFFERENT)
                .difficulty(Scenario.Difficulty.MEDIUM)
                .industry(Scenario.Industry.SOFTWARE_B2B)
                .maxDurationMinutes(30)
                .productContext("ERP para PyMEs: facturacion, stock, RRHH. $800/mes por 5 usuarios. Demo gratuita 30 dias.")
                .systemPrompt("Sos Roberto Paz, dueno de una ferreteria con 15 empleados. Tenes 58 anos y no sos muy tecnologico. Tu negocio funciona bien con planillas y papel. No ves para que cambiar lo que funciona. Hablas en espanol rioplatense. Sos educado pero desinteresado. Respondes con monosilabos si no te convencen. Si el vendedor te muestra un beneficio muy concreto en pesos (ahorro de tiempo = plata), prestas mas atencion. Tu mayor miedo es que el sistema falle justo en epoca de inventario.")
                .objectionsGuide("{\"objections\": [\"Nosotros con planillas de Excel nos manejamos re bien\", \"No tengo tiempo para aprender software nuevo\", \"Es muy caro para lo que ofrece\"]}")
                .paymentInfo("Suscripcion mensual o anual con 20% de descuento. Sin permanencia minima.")
                .faq("{\"preguntas\": [\"Tiene integracion con AFIP?\", \"Puedo migrar mis datos de Excel?\"]}")
                .avatarVoiceId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"todo el mundo lo usa\",\"es muy fácil\",\"en cinco minutos lo aprendés\",\"esto lo maneja hasta un nene\",\"no tiene pérdida\",\"no hay nada que aprender\"]")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente exigente - Consultoria de RRHH")
                .description("Directora de RRHH de una empresa mediana que exige resultados medibles y ROI claro.")
                .clientPersona(Scenario.ClientPersona.DEMANDING)
                .difficulty(Scenario.Difficulty.HARD)
                .industry(Scenario.Industry.CONSULTORIA)
                .maxDurationMinutes(45)
                .productContext("Consultoria de RRHH: seleccion, capacitacion y clima organizacional. Proyectos desde $5000.")
                .systemPrompt("Sos Valeria Torres, Directora de RRHH de una empresa de 200 personas. Sos MBA, muy analitica y exigente. Hablas en espanol rioplatense. Haces preguntas dificiles sobre metodologia, tiempos y metricas. Esperas respuestas concretas con numeros. Si el vendedor es vago, lo presionas mas. Tuviste una mala experiencia con una consultora anterior que prometio mucho y entrego poco. Solo cerras si el vendedor puede darte casos de exito verificables y un cronograma detallado.")
                .objectionsGuide("{\"objections\": [\"Que ROI concreto me garantizan?\", \"Necesito metricas claras antes de contratar\", \"Tuvimos una consultora antes y fue un fracaso\"]}")
                .paymentInfo("50% al inicio, 50% al finalizar el proyecto. Garantia de satisfaccion 30 dias.")
                .faq("{\"preguntas\": [\"Cuanto tiempo lleva el proceso de seleccion?\", \"Tienen experiencia en mi industria?\"]}")
                .avatarVoiceId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights("{\"persuasion\":20,\"product_knowledge\":25,\"objection_handling\":30,\"fluency\":10,\"confidence\":15}")
                .forbiddenPhrases("[\"confíe en nosotros\",\"somos los mejores del mercado\",\"todos nuestros clientes están felices\",\"eso lo podemos ver después\",\"no se preocupe por eso\",\"es un estándar de la industria\"]")
                .createdBy("MANUAL")
                .build(),

            Scenario.builder()
                .name("Cliente dificil - Producto financiero")
                .description("Un inversor esceptico que desconfia de los productos financieros luego de malas experiencias.")
                .clientPersona(Scenario.ClientPersona.DIFFICULT)
                .difficulty(Scenario.Difficulty.MEDIUM)
                .industry(Scenario.Industry.FINANZAS)
                .maxDurationMinutes(30)
                .productContext("Fondo de inversion de renta fija: TNA 45%, liquidez en 72hs, minimo $100.000.")
                .systemPrompt("Sos Marcelo Gimenez, inversor particular de 52 anos. Perdiste dinero en 2001 y con una financiera en 2019. Desconfias de todos los productos financieros. Hablas en espanol rioplatense. Haces preguntas tecnicas sobre regulacion, garantias y riesgo. Buscas contradicciones en lo que dice el vendedor. Si el vendedor menciona la CNV, BCRA o garantias concretas, bajas un poco la guardia. Necesitas sentir que el vendedor entiende tus miedos antes de considerar invertir.")
                .objectionsGuide("{\"objections\": [\"Ya me estafaron con un fondo parecido\", \"Esta regulado por la CNV?\", \"El plazo fijo me da mas tranquilidad\"]}")
                .paymentInfo("Sin comision de entrada. Comision de administracion: 1.5% anual sobre el capital.")
                .faq("{\"preguntas\": [\"Que pasa si necesito el dinero antes?\", \"Esta garantizado el capital?\"]}")
                .avatarVoiceId("TAVUS_PERSONA_ID_PLACEHOLDER")
                .evaluationWeights(defaultWeights)
                .forbiddenPhrases("[\"garantizado al 100%\",\"no hay riesgo\",\"imposible perder\",\"todos invierten en esto\",\"el gobierno lo respalda\",\"yo también invierto aquí\"]")
                .createdBy("MANUAL")
                .build()
        );
    }
}
