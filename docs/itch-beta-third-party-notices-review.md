# Nexus Tycoon itchBeta — revisão de avisos de terceiros

Estado em 23/10/2026: **inventário de revisão, não aviso final de distribuição**. A lista parte das dependências declaradas em `app/build.gradle.kts` e `gradle/libs.versions.toml`; não houve resolução completa do grafo de runtime ou auditoria de cada AAR/JAR transitive nesta etapa.

| Grupo declarado | Evidência de licença/termos | Ação antes da distribuição |
| --- | --- | --- |
| AndroidX: Compose, Material Icons, Activity, Lifecycle, Room, Core, Credentials | [Licença do projeto AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt), Apache-2.0 | Conferir versões/artefatos empacotados; fornecer licença e qualquer `NOTICE` aplicável. |
| Kotlin e kotlinx.coroutines | [Kotlin](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) e [coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt), Apache-2.0 | Conferir módulos em runtime; fornecer licença e avisos aplicáveis. |
| Retrofit, OkHttp, Moshi | [Retrofit](https://github.com/square/retrofit/blob/trunk/LICENSE.txt), [OkHttp](https://github.com/square/okhttp/blob/master/LICENSE.txt), [Moshi](https://github.com/square/moshi/blob/master/LICENSE.txt) | Conferir metadados das versões resolvidas e os avisos de cada artefato; não presumir que a dependência declarada esteja integralmente no APK. |
| Firebase Auth/Firestore, Google Identity/Play Services | SDKs declarados e usados para login/backup opcionais | Conferir os termos e avisos das versões efetivamente empacotadas, além da informação de fornecedores na política. |
| AdMob/UMP e Google Play Billing | Dependências declaradas, com controles comerciais bloqueados na `itchBeta` | Confirmar artefatos efetivos e avisos aplicáveis; não anunciar monetização nessa variante. |
| Testes: JUnit, Robolectric, Roborazzi e AndroidX Test | Dependências de teste declaradas | Não incluir em aviso de runtime sem confirmar presença no APK; verificar somente se forem redistribuídas. |

Os efeitos `sfx_*.wav` têm declaração de síntese procedural própria e fonte em `tools/generate_sfx.py`, documentadas em `SOUND_ASSETS.md`. As imagens têm origem geral por IA declarada pelo proprietário, com ferramenta individual majoritariamente não confirmada; o inventário visual registra essa limitação. Esses itens não substituem os avisos de bibliotecas.

Para fechar: obter o grafo `itchBetaRuntimeClasspath` sem refazer a build, identificar licenças e arquivos `NOTICE` das versões resolvidas, preparar o texto completo de avisos que acompanhe o APK/página e revisar manualmente. Esta etapa identificou famílias e fontes, **não** confirmou que todos os avisos obrigatórios estão prontos.
