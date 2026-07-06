# language: fr
Fonctionnalité: Exclusion logback-classic des classpaths de test
  En tant que développeur de plugin dans le workspace cccp-education
  Je veux appliquer la convention d'exclusion logback
  Afin que mes tests n'entrent pas en conflit avec le JRuby embarqué par asciidoctor

  Scénario: Le plugin exclut logback-classic des configurations de test
    Etant donné un projet qui applique la convention d'exclusion logback
    Alors la configuration "testRuntimeClasspath" exclut logback-classic sans exclure slf4j
    Et la configuration "testImplementation" exclut logback-classic sans exclure slf4j
    Et la configuration "functionalTestRuntimeClasspath" exclut logback-classic sans exclure slf4j

  Scénario: Le plugin préserve slf4j-api dans les configurations de test
    Etant donné un projet qui applique la convention d'exclusion logback
    Alors la configuration "testImplementation" n'exclut pas slf4j-api