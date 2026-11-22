import { Activity } from 'lucide-react';

const ITEMS = [
  { key: 'positive', label: 'Positive', color: 'var(--green)' },
  { key: 'neutral', label: 'Neutral', color: 'var(--blue)' },
  { key: 'negative', label: 'Negative', color: 'var(--rose)' },
];

export default function SentimentPanel({ counts = {}, percentages = {}, loading = false, unavailable = false }) {
  const total = ITEMS.reduce((sum, item) => sum + (Number(counts[item.key]) || 0), 0);
  let cursor = 0;
  const segments = ITEMS.map((item) => {
    const percent = total ? ((Number(counts[item.key]) || 0) / total) * 100 : 0;
    const start = cursor;
    cursor += percent;
    return `${item.color} ${start}% ${cursor}%`;
  });
  const ring = total
    ? `conic-gradient(${segments.join(', ')})`
    : 'conic-gradient(#e8ebf2 0% 100%)';

  return (
    <article className="sentiment-panel">
      <div className="panel-heading">
        <div className="panel-heading-icon"><Activity size={17} /></div>
        <div><h3>Sentiment breakdown</h3><p>How customers feel about your products</p></div>
        <span className="period-pill">All time</span>
      </div>
      <div className="sentiment-body">
        <div className="donut-wrap" aria-label={total ? `Sentiment breakdown of ${total} analyzed reviews` : 'No sentiment data yet'}>
          <div className="donut-chart" style={{ background: ring }}>
            <div className="donut-hole">
              {loading ? <span className="skeleton donut-skeleton" /> : unavailable ? <><strong>—</strong><span>unavailable</span></> : <><strong>{total.toLocaleString()}</strong><span>analyzed</span></>}
            </div>
          </div>
        </div>
        <div className="sentiment-legend">
          {ITEMS.map((item) => (
            <div className="legend-row" key={item.key}>
              <span className="legend-dot" style={{ background: item.color }} />
              <span className="legend-label">{item.label}</span>
              <strong>{loading || unavailable ? '—' : (Number(counts[item.key]) || 0).toLocaleString()}</strong>
              <span className="legend-percent">{loading || unavailable || percentages[item.key] == null ? '—' : `${Number(percentages[item.key]) || 0}%`}</span>
            </div>
          ))}
        </div>
      </div>
      {!loading && unavailable && <p className="sentiment-empty-note">Connect to the backend to load sentiment insights.</p>}
      {!loading && !unavailable && total === 0 && <p className="sentiment-empty-note">Upload reviews to see sentiment insights.</p>}
    </article>
  );
}
