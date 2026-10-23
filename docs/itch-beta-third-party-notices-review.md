# Nexus Tycoon itchBeta — revisão de avisos de terceiros

Estado em 23/10/2026: **inventário de revisão, não aviso final de distribuição**. Além das dependências declaradas, a configuração `itchBetaRuntimeClasspath` foi resolvida com o Gradle em modo offline, sem recompilar o APK. O resultado confirma as famílias abaixo no grafo de runtime, mas não prova quais classes/recursos sobreviveram ao empacotamento nem audita cada AAR/JAR transitivo.

| Grupo declarado | Evidência de licença/termos | Ação antes da distribuição |
| --- | --- | --- |
| AndroidX: Compose, Material Icons, Activity, Lifecycle, Room, Core, Credentials | [Licença do projeto AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt), Apache-2.0 | Conferir versões/artefatos empacotados; fornecer licença e qualquer `NOTICE` aplicável. |
| Kotlin e kotlinx.coroutines | [Kotlin](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) e [coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt), Apache-2.0 | Conferir módulos em runtime; fornecer licença e avisos aplicáveis. |
| Retrofit, OkHttp, Moshi | [Retrofit](https://github.com/square/retrofit/blob/trunk/LICENSE.txt), [OkHttp](https://github.com/square/okhttp/blob/master/LICENSE.txt), [Moshi](https://github.com/square/moshi/blob/master/LICENSE.txt) | Conferir metadados das versões resolvidas e os avisos de cada artefato; não presumir que a dependência declarada esteja integralmente no APK. |
| Firebase Auth/Firestore, Google Identity/Play Services | SDKs declarados e usados para login/backup opcionais | Conferir os termos e avisos das versões efetivamente empacotadas, além da informação de fornecedores na política. |
| AdMob/UMP e Google Play Billing | Dependências declaradas, com controles comerciais bloqueados na `itchBeta` | Confirmar artefatos efetivos e avisos aplicáveis; não anunciar monetização nessa variante. |
| Testes: JUnit, Robolectric, Roborazzi e AndroidX Test | Dependências de teste declaradas | Não incluir em aviso de runtime sem confirmar presença no APK; verificar somente se forem redistribuídas. |

Os efeitos `sfx_*.wav` têm declaração de síntese procedural própria e fonte em `tools/generate_sfx.py`, documentadas em `SOUND_ASSETS.md`. As imagens têm origem geral por IA declarada pelo proprietário, com ferramenta individual majoritariamente não confirmada; o inventário visual registra essa limitação. Esses itens não substituem os avisos de bibliotecas.

**Versões diretas observadas no grafo resolvido:** Kotlin stdlib 2.2.10; AndroidX Credentials 1.3.0, Activity Compose 1.10.1, Compose UI 1.7.0, Material3 1.3.0, Room 2.7.0, Core KTX 1.18.0, Lifecycle 2.8.7; coroutines 1.10.2; Firebase Auth 24.0.1 e Firestore 26.2.0; Google Play Services Auth 21.1.1; Ads Mobile SDK 1.5.0, UMP 4.0.0 e Billing 8.3.0; Retrofit 2.12.0, OkHttp 4.11.0 e Moshi 1.15.2. BOMs e artefatos de teste não devem ser tratados como binários distribuídos apenas por constarem no relatório.

**Para fechar antes da distribuição:** extrair os metadados de licença e eventuais arquivos `NOTICE` de **todos os artefatos efetivamente empacotados**, incluir os textos exigidos (em especial o texto integral Apache-2.0 e avisos dos componentes que o exigirem), conferir os termos dos SDKs Google e revisar o pacote final. A [licença Apache-2.0 oficial](https://www.apache.org/licenses/LICENSE-2.0) fornece o texto de referência. Esta inspeção melhorou o inventário, mas **não confirmou que os avisos obrigatórios estão completos**; publicar apenas com a tabela acima seria prematuro.
