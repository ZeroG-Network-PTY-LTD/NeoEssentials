import { usePalette } from '@universemc/react-palette';

/**
 * Full-body 3D-style Minecraft character render with a color-matched animated
 * gradient blob behind it — ported from the external dashboard's identical
 * component (vzge.me render + @universemc/react-palette color extraction,
 * same approach as BanManager WebUI's PlayerAvatar).
 */
export default function PlayerRender({
  uuid,
  size = 160,
  className = '',
}: {
  uuid: string | null | undefined;
  /** Height of the character render itself, in px. */
  size?: number;
  className?: string;
}) {
  // vzge.me renders at whatever pixel height you ask for — request 2x for a
  // crisp image on high-DPI screens without inflating the DOM element itself.
  const src = uuid ? `https://vzge.me/full/${Math.round(size * 2)}/${uuid}.png` : '';
  const { data: colour } = usePalette(src);

  if (!uuid) return null;

  const backgroundStyle = {
    backgroundImage: `linear-gradient(-45deg, ${colour.vibrant ?? 'var(--mc-cyan-500)'}, ${colour.darkVibrant ?? 'var(--mc-purple-500)'}, var(--mc-cyan-500))`,
  };

  return (
    <div className={`relative inline-flex items-center justify-center ${className}`} style={{ height: size }}>
      <div
        className="animate-player-gradient absolute rounded-full opacity-40 blur-2xl"
        style={{ ...backgroundStyle, width: size * 0.9, height: size * 0.9 }}
      />
      <img
        src={src}
        alt=""
        className="relative z-10 [image-rendering:pixelated] drop-shadow-[0.4rem_0.4rem_0.9rem_rgba(0,0,0,0.55)]"
        style={{ height: size }}
      />
    </div>
  );
}
