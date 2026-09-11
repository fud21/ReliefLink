export default function PageHeader({ eyebrow, title, description, right }) {
  return (
    <div className="page-header">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {right && <div>{right}</div>}
    </div>
  );
}
