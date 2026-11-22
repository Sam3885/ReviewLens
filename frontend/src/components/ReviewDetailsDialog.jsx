import { useEffect, useState } from 'react';
import { Check, Clipboard, Copy, LoaderCircle, MessageSquareText, Star, X } from 'lucide-react';
import { errorMessage } from '../api/client.js';
import { SentimentBadge, StatusBadge } from './ReviewsSection.jsx';

export default function ReviewDetailsDialog({ review, loading, error, replyError: initialReplyError = '', onClose, onGenerateReply }) {
  const [replyLoading, setReplyLoading] = useState(false);
  const [replyError, setReplyError] = useState('');
  const [copied, setCopied] = useState(false);
  const analysis = review?.analysis;
  const needsReply = Boolean(analysis?.needs_reply);

  useEffect(() => { setReplyError(initialReplyError); }, [initialReplyError]);

  useEffect(() => {
    const onKeyDown = (event) => { if (event.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  const generateReply = async () => {
    if (!review || !needsReply || replyLoading) return;
    setReplyLoading(true);
    setReplyError('');
    setCopied(false);
    try {
      await onGenerateReply(review.reviewId);
    } catch (requestError) {
      setReplyError(errorMessage(requestError));
    } finally {
      setReplyLoading(false);
    }
  };

  const copyReply = async () => {
    try {
      await navigator.clipboard.writeText(review?.suggestedReply || '');
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setReplyError('Clipboard access is unavailable. Select and copy the reply text instead.');
    }
  };

  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="review-dialog" role="dialog" aria-modal="true" aria-labelledby="review-dialog-title">
        <div className="dialog-topline"><span className="dialog-eyebrow">REVIEW DETAILS</span>
          <button type="button" className="icon-button dialog-close" aria-label="Close review details" onClick={onClose}><X size={18} /></button></div>
        {loading ? <div className="dialog-loading"><LoaderCircle className="spin" size={24} /><span>Loading review details…</span></div>
          : error ? <div className="dialog-error" role="alert">{error}</div>
            : review && <>
              <div className="dialog-title-row"><div><h2 id="review-dialog-title">{review.productName || 'Review'}</h2><span className="dialog-review-id">Review ID · {review.reviewId}</span></div>
                <StatusBadge value={review.analysisStatus} /></div>
              <div className="detail-meta"><span className="detail-rating"><Star size={15} fill="currentColor" /> {review.rating ?? 'No rating'}{review.rating != null && ' / 5'}</span><span>{review.reviewDate || 'Date unavailable'}</span></div>
              <div className="detail-review-text"><span className="detail-label">CUSTOMER REVIEW</span><p>{review.reviewText || 'No review text.'}</p></div>

              {analysis ? <div className="detail-analysis">
                <div className="detail-analysis-head"><span className="detail-label">AI ANALYSIS</span><SentimentBadge value={analysis.sentiment} /></div>
                <div className="detail-field"><span className="detail-label">SUMMARY</span><p>{analysis.summary || 'No summary available.'}</p></div>
                <div className="detail-field"><span className="detail-label">TOPICS</span><div className="topic-chips">{(analysis.topics || []).map((topic) => <span className="topic-chip" key={topic}>{topic.replaceAll('_', ' ')}</span>)}</div></div>
                {needsReply && <div className="reply-needed-note"><MessageSquareText size={15} /> This review is marked for a response.</div>}
              </div> : <div className={`detail-analysis-state ${String(review.analysisStatus).toLowerCase()}`}>
                {String(review.analysisStatus).toLowerCase() === 'analysis_failed'
                  ? 'Analysis failed for this review. It can be retried by uploading the CSV again.'
                  : 'Analysis is not available yet.'}
              </div>}

              {needsReply && <div className="suggested-reply-box">
                <div className="suggested-reply-heading"><div><span className="detail-label">SUGGESTED REPLY</span><p>Generate when you are ready to respond.</p></div>
                  {!review.suggestedReply && <button type="button" className="button button-primary button-small" disabled={replyLoading} onClick={generateReply}>
                    {replyLoading ? <LoaderCircle className="spin" size={15} /> : <MessageSquareText size={15} />}
                    {replyLoading ? 'Generating…' : 'Generate reply'}
                  </button>}
                </div>
                {review.suggestedReply && <div className="suggested-reply-content"><p>{review.suggestedReply}</p><button className="button button-secondary button-small" onClick={copyReply} type="button">{copied ? <Check size={14} /> : <Copy size={14} />}{copied ? 'Copied' : 'Copy reply'}</button></div>}
                {replyError && <div className="inline-error" role="alert"><X size={14} />{replyError}</div>}
              </div>}
            </>}
        <div className="dialog-footer"><span><Clipboard size={13} /> ReviewLens analysis · verify before sending</span><button type="button" className="button button-secondary button-small" onClick={onClose}>Close</button></div>
      </section>
    </div>
  );
}
