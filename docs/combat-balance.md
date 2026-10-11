# Rebalanceamento da mineração hostil

## Comparação antes da implementação

Créditos por vitória (atual → proposta/final):

| Ameaça | Setor 1 | Setor 2 | Setor 3 | Setor 4 |
|---|---:|---:|---:|---:|
| 1 | 2500 → 500 | 5000 → 1000 | 7500 → 1500 | 10000 → 2500 |
| 10 | 3850 → 770 | 7700 → 1540 | 11550 → 2310 | 15400 → 3850 |
| 11 | 4000 → 800 | 8000 → 1600 | 12000 → 2400 | 16000 → 4000 |
| 20 | 5350 → 1070 | 10700 → 2140 | 16050 → 3210 | 21400 → 5350 |
| 50 | 9850 → 1970 | 19700 → 3940 | 29550 → 5910 | 39400 → 9850 |

A fórmula antiga era setor × 2500 × (1 + 0,06 × vitórias). A nova usa bases [500, 1000, 1500, 2500] com a mesma progressão de 6%. Diamantes antigos: floor(setor × 20 × (1 + 0,05 × vitórias)). Diamantes novos, iguais em todos os setores: 1 + 2 × floor((ameaça − 1) / 10). Guild tokens mantêm floor(setor × 5 × (1 + 0,05 × (ameaça − 1))). Nenhum saldo existente é recalculado.

## Custos e indústria usados na decisão

Os custos de unidades são base × 1,3^(nível − 1), multiplicados pelo desconto de Logística Tática. Sem esse desconto e com saldo inicial zero:

| Melhoria | Custo | Setor / ameaça inicial | Vitórias antes → depois |
|---|---:|---|---:|
| Titan 1→2 | 120 | 1 / 1 | 1 → 1 |
| Plasma 10→11 | 5302,25 | 1 / 10 | 2 → 7 |
| EMP 20→21 | 175430,43 | 4 / 20 | 8 → 25 |

Essas contagens somam recompensas de vitórias consecutivas, aumentando a ameaça a cada vitória. Não incluem renda industrial, despesas intermediárias ou derrotas, e não afirmam que a mesma equipe vence toda a sequência. Com desconto estelar, o custo cai até 20%.

As três primeiras fábricas automatizadas nos níveis 1, 10 e 25 produzem estoque líquido com valor de venda de aproximadamente 22,5, 470 e 2400 créditos/s respectivamente. Cálculo sem cores, cientistas de produção, ciência, VIP ou volatilidade de mercado, usando preços-base Energy Cell=1, Iron Ore=5 e Hyperalloy=15 e descontando insumos consumidos. No nível 10: 35 células + 27 minérios + 20 ligas por segundo. No nível 25: 187,5 células + 142,5 minérios + 100 ligas. Estoque não é dinheiro automático: pressupõe venda e automação adquirida.

Decisão: manter a proposta. Reduz 80% dos créditos dos setores 1–3 e 75% do setor 4. O combate pode ser uma ajuda forte na abertura, mas exige atividade, enfrenta ameaça crescente (+15% HP e +12% dano por vitória, preservados) e as melhorias de unidades crescem exponencialmente. A indústria recebe marcos multiplicadores e continua produzindo durante os combates. Essa análise estática não comprova duração de sessão ou ritmo ideal em aparelho.

## Persistência e pagamento

Room 11 adiciona somente combat_battles. O registro contém UUID, setor, ameaça enfrentada, recompensas e status. A concessão lê o registro e o GameState atuais dentro da mesma transação, marca a batalha concluída e credita os saldos/contador do setor. Derrota encerra o registro sem pagamento. Nova batalha abandona registros pendentes anteriores; não há cooldown ou limite diário. Fechar a tela cancela o laço anterior. Uma vitória por habilidade e pelo laço não pode pagar duas vezes.

A tela de seleção usa CombatRewards.forLevel. O início usa a mesma função e congela seus valores; resultado e crédito usam esse registro. O nível considerado sempre precede o incremento de vitórias. Inimigos, evolução, habilidades e desbloqueio progressivo mantêm suas fórmulas.

Backups de progresso continuam v3 e aceitam v2. Registros operacionais de batalhas são locais, não exportados. Restaurar um backup invalida batalhas pendentes, evitando pagar uma batalha iniciada antes da restauração. O registro de uma batalha concluída sobrevive ao reinício do aplicativo. A retomada visual de combate após encerramento do processo não foi adicionada.

Validação: assembleDebug, testDebugUnitTest e lintDebug concluídos; 38 testes passaram, incluindo seis de CombatRewardsTest. Sem teste em aparelho.
