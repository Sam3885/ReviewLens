import { useState } from 'react';
import { RotateCcw, SlidersHorizontal } from 'lucide-react';
import FilterSelect, { TOPICS } from './FilterSelect.jsx';

const SENTIMENTS = ['positive', 'neutral', 'negative'];

export default function DashboardFilters({ value, onApply }) {
  const [draft, setDraft] = useState(value);
  const hasFilters = Object.values(draft).some(Boolean) || Object.values(value).some(Boolean);
  const update = (key, next) => setDraft((current) => ({ ...current, [key]: next }));

  const reset = () => {
    const empty = { product: '', sentiment: '', topic: '' };
    setDraft(empty);
    onApply(empty);
  };

  return (
    <form className="dashboard-filters" onSubmit={(event) => { event.preventDefault(); onApply(draft); }}>
      <div className="dashboard-filter-title"><SlidersHorizontal size={15} /><span>Filter insights</span></div>
      <label className="filter-control dashboard-product-filter" htmlFor="dashboard-product-filter">
        <span>Product</span>
        <input id="dashboard-product-filter" type="search" placeholder="All products" value={draft.product}
          onChange={(event) => update('product', event.target.value)} />
      </label>
      <FilterSelect id="dashboard-sentiment-filter" label="Sentiment" value={draft.sentiment}
        onChange={(value) => update('sentiment', value)} options={SENTIMENTS} />
      <FilterSelect id="dashboard-topic-filter" label="Topic" value={draft.topic}
        onChange={(value) => update('topic', value)} options={TOPICS} />
      <div className="dashboard-filter-actions">
        {hasFilters && <button type="button" className="text-button reset-button" onClick={reset}><RotateCcw size={13} /> Reset</button>}
        <button type="submit" className="button button-secondary button-small">Apply</button>
      </div>
    </form>
  );
}
