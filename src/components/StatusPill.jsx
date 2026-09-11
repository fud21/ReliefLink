export default function StatusPill({
  children,
  tone = "blue"
}) {
  return (
    <span className={`status-pill ${tone}`}>
      {children}
    </span>
  );
}