import { ArrowUpRight, CircleHelp } from 'lucide-react';

const sentimentTone = {
  positive: 'green',
  neutral: 'blue',
  negative: 'rose',
  reply: 'violet',
  total: 'ink',
};

export default function MetricCard({ label, value, detail, icon: Icon, tone = 'ink', loading = false }) {
  return (
    <article className={`metric-card metric-${sentimentTone[tone] || tone}`}>
      <div className="metric-top">
        <span className="metric-label">{label}</span>
        <span className="metric-icon">{Icon ? <Icon size={17} strokeWidth={1.9} /> : <CircleHelp size={16} />}</span>
      </div>
      {loading ? <div className="skeleton metric-skeleton" /> : <div className="metric-value">{value}</div>}
      <div className="metric-detail">{loading ? <span className="skeleton detail-skeleton" /> : <>{detail}<ArrowUpRight size={13} aria-hidden="true" /></>}</div>
    </article>
  );
}
