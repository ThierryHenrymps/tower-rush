# Tower Rush — Identidade sonora

Todos os sons foram **sintetizados por código** (`generate_audio.py`, Python + NumPy/SciPy):
ondas senoidais, ruído, filtros e reverb calculado. **Nenhum sample, gravação, música ou
biblioteca de sons de terceiros** foi usado, e nenhuma melodia existente foi reproduzida.

Formato de todos os arquivos: WAV PCM 16-bit, 44,1 kHz, estéreo.

| Arquivo | Duração | Pico | Uso |
|---|---|---|---|
| `block_place.wav` | 0,70 s | −4 dBFS | bloco encaixa na torre |
| `magic_event.wav` | 3,60 s | −2,5 dBFS | evento mágico / power-up |
| `new_record.wav` | 3,20 s | −2,5 dBFS | novo recorde |
| `background_music.wav` | 45,71 s | −8 dBFS (RMS ≈ −20) | trilha em loop |

## Características

**block_place.wav** — "plup" macio: um golpe grave suave (pitch caindo de ~180 para ~85 Hz)
somado a um "pop" de bolha (pitch subindo de ~270 para ~520 Hz), com um clique mínimo e uma
sala bem curta. Pico mais baixo que os demais por ser tocado com muita frequência.

**magic_event.wav** — brilho ascendente (vozes com glissando e "ar" crescente), arpejo de sinos
cristalinos em pentatônica de Dó (C6→E7), uma pequena explosão de energia (impacto grave +
rajada de ruído + acorde de sinos) e faíscas aleatórias que se dissipam num reverb brilhante.

**new_record.wav** — corrida ascendente alegre (C5 E5 G5 C6 E6), nota de impulso (G6) e acorde
final de Dó maior com C7, sinos e faíscas. Timbre de pluck brilhante, tipo marimba/synth.

**background_music.wav** — ambiente relaxante e levemente mágico, 84 BPM, 16 compassos, Dó
maior com cor lídia (Fmaj7#11). Piano suave (arpejos de colcheias + melodia original em
pentatônica), pads de sintetizador, baixo senoidal, shaker quase imperceptível e sinos que
aparecem de forma esparsa na 1ª volta e dobram a melodia na 2ª. O loop é circular: a cauda do
reverb e das notas é dobrada sobre o início, então não há corte nem clique na emenda
(salto medido na emenda menor que o passo médio entre amostras).

## Licença de uso

- Não há material de terceiros nos arquivos, então não existe licença externa a cumprir
  (sem atribuição, sem royalties de samples ou bibliotecas).
- Os arquivos e o script foram criados sob demanda para o projeto Tower Rush. Pelos termos de
  uso da Anthropic vigentes, os outputs gerados para você são seus para usar, inclusive
  comercialmente no jogo. Confira os termos atuais; esta nota não é aconselhamento jurídico.
- A proteção por direito autoral de conteúdo gerado por IA/processos automáticos varia por
  país. Se for registrar a trilha ou publicá-la em plataformas de música, vale consultar
  um advogado.
- Se quiser distribuir o script publicamente, escolha você mesmo a licença (por exemplo
  MIT ou CC0); ele não traz nenhuma por padrão.

## Regenerar / ajustar

    pip install numpy scipy
    python3 generate_audio.py

A semente aleatória é fixa (2026), então o resultado é reproduzível. Andamento, acordes e
melodia da trilha ficam em `background_music()`.

## Dicas para Android

- Coloque em `res/raw/` (os nomes já são válidos: minúsculos com underscore).
- Efeitos curtos: `SoundPool`. Trilha: `MediaPlayer` com `setLooping(true)`.
- A trilha em WAV tem ~8 MB; para reduzir o APK, converta para OGG (o loop continua válido,
  mas teste a emenda, pois alguns codecs com perdas adicionam um pequeno silêncio).
