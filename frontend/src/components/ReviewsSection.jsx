import { ArrowDownWideNarrow, ChevronLeft, ChevronRight, Eye, Filter, MessageCircle, RotateCw, Star } from 'lucide-react';
import FilterSelect, { TOPICS } from './FilterSelect.jsx';

const SENTIMENTS = ['positive', 'neutral', 'negative'];

export default function ReviewsSection({
  reviews = [], total = 0, page = 0, totalPages = 0, pageSize = 10,
  filters, onFilterChange, onApplyFilters, onClearFilters, onPageChange,
  onViewReview, onSuggestReply, loading, error,
}) {
  const hasFilters = Object.values(filters).some(Boolean);

  return (
    <section className="reviews-section" id="reviews">
      <div className="section-header reviews-header">
        <div>
          <div className="section-kicker">VOICE OF CUSTOMER</div>
          <h2>Review library</h2>
          <p>Browse, filter and respond to customer feedback.</p>
        </div>
        <div className="review-total"><strong>{total.toLocaleString()}</strong><span>reviews</span></div>
      </div>

      <form className="review-filters" onSubmit={(event) => { event.preventDefault(); onApplyFilters(); }}>
        <div className="filter-label"><Filter size={15} /> Filters</div>
        <label className="filter-control product-filter" htmlFor="review-product-filter">
          <span>Product</span>
          <input id="review-product-filter" type="search" placeholder="Search products" value={filters.product}
            onChange={(event) => onFilterChange('product', event.target.value)} />
        </label>
        <FilterSelect id="review-sentiment-filter" label="Sentiment" value={filters.sentiment}
          onChange={(value) => onFilterChange('sentiment', value)} options={SENTIMENTS} />
        <FilterSelect id="review-topic-filter" label="Topic" value={filters.topic}
          onChange={(value) => onFilterChange('topic', value)} options={TOPICS} />
        <div className="filter-actions">
          <button className="button button-secondary button-small" type="submit">Apply filters</button>
          {hasFilters && <button className="text-button" type="button" onClick={onClearFilters}>Clear</button>}
        </div>
      </form>

      {error && <div className="section-error" role="alert"><span>{error}</span><button type="button" className="icon-button" onClick={onApplyFilters} aria-label="Retry loading reviews"><RotateCw size={15} /></button></div>}

      <div className="reviews-table-wrap">
        <table className="reviews-table">
          <thead><tr>
            <th scope="col"><span className="table-head-label">PRODUCT</span></th>
            <th scope="col"><span className="table-head-label">REVIEW</span></th>
            <th scope="col"><span className="table-head-label">SENTIMENT</span></th>
            <th scope="col"><span className="table-head-label">TOPICS</span></th>
            <th scope="col"><span className="table-head-label">ANALYSIS</span></th>
            <th scope="col"><span className="table-head-label"><ArrowDownWideNarrow size={13} /> STATUS</span></th>
            <th scope="col"><span className="visually-hidden">Actions</span></th>
          </tr></thead>
          <tbody>
            {loading ? <LoadingRows /> : reviews.map((review) => (
              <ReviewRow key={review.reviewId} review={review} onView={() => onViewReview(review.reviewId)}
                onReply={() => onSuggestReply(review.reviewId)} />
            ))}
          </tbody>
        </table>
        {!loading && !reviews.length && <div className="table-empty">
          <div className="empty-icon"><MessageCircle size={19} /></div>
          <strong>{hasFilters ? 'No reviews match these filters' : 'No reviews to show yet'}</strong>
          <p>{hasFilters ? 'Try changing or clearing a filter.' : 'Upload a CSV to add customer reviews to your library.'}</p>
          {hasFilters && <button className="text-button" onClick={onClearFilters}>Clear filters</button>}
        </div>}
      </div>

      <div className="table-footer">
        <span>Showing {reviews.length ? page * pageSize + 1 : 0}–{Math.min((page + 1) * pageSize, total)} of {total.toLocaleString()} reviews</span>
        <div className="pagination-controls">
          <button type="button" className="icon-button" aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => onPageChange(page - 1)}><ChevronLeft size={17} /></button>
          <span>Page <strong>{totalPages ? page + 1 : 0}</strong> of <strong>{totalPages}</strong></span>
          <button type="button" className="icon-button" aria-label="Next page" disabled={page + 1 >= totalPages || loading} onClick={() => onPageChange(page + 1)}><ChevronRight size={17} /></button>
        </div>
      </div>
    </section>
  );
}

function ReviewRow({ review, onView, onReply }) {
  const analysis = review.analysis;
  const status = String(review.analysisStatus || 'pending').toLowerCase();
  const needsReply = Boolean(analysis?.needs_reply);
  return (
    <tr className={status === 'analysis_failed' ? 'failed-row' : ''}>
      <td data-label="Product">
        <div className="product-cell"><span className="product-initial">{(review.productName || '?').trim().charAt(0).toUpperCase()}</span>
          <div><strong>{review.productName || 'Unnamed product'}</strong><span className="rating-line"><Star size={12} fill="currentColor" /> {review.rating ?? '—'} / 5</span></div></div>
      </td>
      <td data-label="Review"><div className="review-copy" title={review.reviewText}>{review.reviewText || '—'}</div><span className="review-date">{review.reviewDate || 'Date unavailable'}</span></td>
      <td data-label="Sentiment">{analysis?.sentiment ? <SentimentBadge value={analysis.sentiment} /> : <span className="muted-dash">—</span>}</td>
      <td data-label="Topics"><div className="topic-chips">{(analysis?.topics || []).slice(0, 2).map((topic) => <span className="topic-chip" key={topic}>{topic.replaceAll('_', ' ')}</span>)}{(analysis?.topics?.length || 0) > 2 && <span className="topic-more">+{analysis.topics.length - 2}</span>}{!analysis?.topics?.length && <span className="muted-dash">—</span>}</div></td>
      <td data-label="Analysis"><span className="summary-copy">{analysis?.summary || (status === 'analysis_failed' ? 'Analysis could not be completed.' : status === 'pending' ? 'Analysis pending.' : '—')}</span></td>
      <td data-label="Status"><StatusBadge value={status} /></td>
      <td data-label="Actions"><div className="row-actions">
        {needsReply && <button type="button" className="row-reply-button" title="Generate suggested reply" aria-label={`Generate suggested reply for ${review.productName}`} onClick={onReply}><MessageCircle size={15} /></button>}
        <button type="button" className="row-view-button" onClick={onView}><Eye size={15} /><span>View</span></button>
      </div></td>
    </tr>
  );
}

export function SentimentBadge({ value }) {
  const sentiment = String(value).toLowerCase();
  return <span className={`sentiment-badge sentiment-${sentiment}`}><span />{sentiment}</span>;
}

export function StatusBadge({ value }) {
  const status = String(value || 'pending').toLowerCase();
  const label = status === 'analysis_failed' ? 'Analysis failed' : status === 'completed' ? 'Complete' : 'Pending';
  return <span className={`status-badge status-${status}`}><span />{label}</span>;
}

function LoadingRows() {
  return Array.from({ length: 5 }, (_, index) => <tr className="loading-row" key={index}>
    <td><span className="skeleton row-skeleton wide" /></td><td><span className="skeleton row-skeleton" /></td>
    <td><span className="skeleton row-skeleton short" /></td><td><span className="skeleton row-skeleton short" /></td>
    <td><span className="skeleton row-skeleton" /></td><td><span className="skeleton row-skeleton short" /></td><td><span className="skeleton row-skeleton short" /></td>
  </tr>);
}
