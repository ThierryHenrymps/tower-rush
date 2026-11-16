# Skin aquática para o Tower Rush — bloco 570 × 167,2 × 353,4

Conteúdo
- `WaterBlockSkin.kt` — skin animada (só Canvas) já nas proporções do seu bloco.
- `MenuWaterTower.kt` — torre da TELA INICIAL com a skin (7 blocos, zigue-zague, base cortada).
- `comparacao_menu.png` / `preview_menu_tower.gif` — sua tela original ao lado da torre com a skin.
- `water_splash.wav` — som de respingo de água (1,8 s, 44,1 kHz, estéreo, pico −3,5 dBFS).
- `generate_water_splash.py` — gera o som (reproduzível).
- `preview_water_skin.gif` / `preview_frame.png` — pré-visualização: 3 blocos empilhados com as medidas pedidas.

## Proporções
- Largura 570, Altura 167,2, Profundidade 353,4 (valores padrão do construtor).
- Padrão: **largura na face esquerda** e **profundidade na face direita**. Se no seu jogo for o
  contrário, chame `draw(..., w = 353.4f, d = 570f)`.
- Projeção padrão 2:1 (`slope = 0.5f`). Para isométrico verdadeiro de 30°, use `slope = 0.577f`.
- Com 2:1, o bloco ocupa na tela 825,9 × 580,2 unidades (`screenWidthUnits` / `screenHeightUnits`).

## Tela inicial ("Clique para jogar")
```kotlin
private val menuTower = MenuWaterTower()          // crie uma vez

// no desenho da tela inicial: depois do fundo/ilhas, ANTES do título, "Clique para jogar" e Store
menuTower.draw(canvas, width.toFloat(), height.toFloat(), SystemClock.uptimeMillis())
```
- O layout foi medido na sua captura de tela: o bloco-base é o 570 × 353,4 (~90% da largura da
  tela), os de cima estreitam e alternam para esquerda/direita, e a base é cortada pela borda.
- Cada bloco tem um tom (`shade`: claro, médio ou escuro) para manter a variação de cor da sua
  torre atual. Ajuste `layout`, `ANCHOR_Y` (altura da torre) e `BASE_WIDTH_FRAC` em `MenuWaterTower.kt`.
- Na comparação, o lado direito usa um fundo roxo e botões só como marcação de posição; o
  arquivo mostra a torre, não a tela final com ilhas, título e textos.
- Custo: 7 blocos × ~50 operações de desenho. Se cair o FPS na tela inicial, use
  `menuTower.skin.lowQuality = true`.

## Como integrar (bloco avulso no jogo)
```kotlin
private val waterSkin = WaterBlockSkin()          // crie uma vez, não a cada frame

// px por unidade do modelo. Ex.: bloco com 360 px de largura na tela:
val scale = 360f / waterSkin.screenWidthUnits     // ≈ 0,436

waterSkin.draw(
    canvas,
    cx, cy,                  // centro do losango do TOPO, em px
    scale,
    SystemClock.uptimeMillis(),
    block.id                 // fase diferente por bloco
)
// empilhar: o bloco seguinte sobe  waterSkin.heightUnits * scale  pixels  (≈ 72,9 px no exemplo)
```
Desenhe do bloco de baixo para o de cima. A animação precisa de redesenho contínuo (o loop do
jogo já faz isso; numa `View` comum use `postInvalidateOnAnimation()`).
`lowQuality = true` desliga onda de fundo, bolhas, ondulações e brilhos em aparelhos fracos.

## Por que é leve
- Sem bitmaps/imagens: não aumenta o APK nem usa memória de textura.
- Nenhum objeto é criado dentro de `draw()` (Paints, Paths, Shaders e arrays reaproveitados).
- ~50 operações simples de desenho por bloco; anime só os blocos visíveis.

## Som (SoundPool, arquivo em `res/raw/water_splash.wav`)
```kotlin
val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
val splashId = pool.load(context, R.raw.water_splash, 1)

// ao encaixar um bloco com a skin de água:
pool.play(splashId, 0.9f, 0.9f, 1, 0, 0.96f + Math.random().toFloat() * 0.08f)
```
Chame `pool.release()` ao sair do jogo.

## Direitos autorais e licença
- Nada aqui usa imagem, som, fonte, biblioteca ou sample de terceiros: o visual é desenhado por
  código próprio (apenas APIs padrão do Android) e o som é sintetizado a partir de ruído e
  senoides. Não há licença externa a pagar nem atribuição obrigatória.
- Pelos termos de uso da Anthropic vigentes, os outputs gerados para você são seus, inclusive
  para uso comercial. Confira os termos atuais; isto não é aconselhamento jurídico, e a proteção
  autoral de conteúdo gerado por IA varia por país.
- Os scripts e o `.kt` não trazem licença por padrão; se for publicá-los, escolha a sua.

## Aviso
O `.kt` não foi compilado aqui (sem compilador Kotlin neste ambiente) nem testado num aparelho.
O GIF vem de uma réplica em Python do mesmo algoritmo: serve para ver o visual e as proporções,
mas cores e desempenho reais você confirma no emulador/celular.
