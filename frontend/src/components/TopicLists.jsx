import { ArrowUpRight, PackageCheck, ThumbsDown, ThumbsUp } from 'lucide-react';

function TopicCard({ title, subtitle, items = [], variant, icon: Icon, unavailable = false, loading = false }) {
  const max = Math.max(1, ...items.map((item) => Number(item.count) || 0));
  return (
    <article className={`topic-card topic-${variant}`}>
      <div className="panel-heading">
        <div className="topic-heading-icon"><Icon size={17} /></div>
        <div><h3>{title}</h3><p>{subtitle}</p></div>
        <span className="heading-kebab" aria-hidden="true">···</span>
      </div>
      {loading && !items.length ? <div className="topic-loading" aria-label="Loading topics"><span /><span /><span /></div> : items.length ? (
        <ol className="topic-list">
          {items.slice(0, 3).map((item, index) => (
            <li key={item.topic}>
              <span className="topic-rank">0{index + 1}</span>
              <span className="topic-name">{item.topic.replaceAll('_', ' ')}</span>
              <span className="topic-count">{Number(item.count).toLocaleString()}</span>
              <span className="topic-bar"><span style={{ width: `${Math.max(8, (Number(item.count) / max) * 100)}%` }} /></span>
            </li>
          ))}
        </ol>
      ) : (
        <div className="topic-empty"><PackageCheck size={19} /><span>{unavailable ? 'Insights unavailable' : 'No analyzed topics yet'}</span></div>
      )}
    </article>
  );
}

export default function TopicLists({ complaints = [], praises = [], unavailable = false, loading = false }) {
  return (
    <div className="topic-grid">
      <TopicCard title="Top complaints" subtitle="Topics in negative reviews" items={complaints} variant="complaints" icon={ThumbsDown} unavailable={unavailable} loading={loading} />
      <TopicCard title="Top praises" subtitle="Topics in positive reviews" items={praises} variant="praises" icon={ThumbsUp} unavailable={unavailable} loading={loading} />
    </div>
  );
}
