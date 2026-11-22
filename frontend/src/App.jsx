import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlertCircle, ArrowUpRight, Frown, Menu, Meh, MessageSquareText, RotateCw, Smile, Sparkles } from 'lucide-react';
import { errorMessage, reviewApi } from './api/client.js';
import Sidebar from './components/Sidebar.jsx';
import MetricCard from './components/MetricCard.jsx';
import SentimentPanel from './components/SentimentPanel.jsx';
import TopicLists from './components/TopicLists.jsx';
import DashboardFilters from './components/DashboardFilters.jsx';
import UploadPanel from './components/UploadPanel.jsx';
import ReviewsSection from './components/ReviewsSection.jsx';
import ReviewDetailsDialog from './components/ReviewDetailsDialog.jsx';

const PAGE_SIZE = 10;
const EMPTY_FILTERS = { product: '', sentiment: '', topic: '' };

export default function App() {
  const [activeNav, setActiveNav] = useState('overview');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [dashboardFilters, setDashboardFilters] = useState(EMPTY_FILTERS);
  const [dashboard, setDashboard] = useState(null);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState('');
  const [reviewFilters, setReviewFilters] = useState(EMPTY_FILTERS);
  const [appliedReviewFilters, setAppliedReviewFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [reviewPage, setReviewPage] = useState({ content: [], totalElements: 0, totalPages: 0, number: 0, size: PAGE_SIZE });
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewsError, setReviewsError] = useState('');
  const [selectedReview, setSelectedReview] = useState(null);
  const dashboardRequest = useRef(0);
  const reviewsRequest = useRef(0);
  const detailsRequest = useRef(0);

  const loadDashboard = useCallback(async (filters = dashboardFilters) => {
    const requestId = ++dashboardRequest.current;
    setDashboardLoading(true);
    setDashboardError('');
    setDashboard(null);
    try {
      const response = await reviewApi.getDashboard(filters);
      if (requestId === dashboardRequest.current) setDashboard(response.data);
    } catch (requestError) {
      if (requestId === dashboardRequest.current) setDashboardError(errorMessage(requestError));
    } finally {
      if (requestId === dashboardRequest.current) setDashboardLoading(false);
    }
  }, [dashboardFilters]);

  const loadReviews = useCallback(async (pageIndex = page, filters = appliedReviewFilters) => {
    const requestId = ++reviewsRequest.current;
    setReviewsLoading(true);
    setReviewsError('');
    setReviewPage({ content: [], totalElements: 0, totalPages: 0, number: pageIndex, size: PAGE_SIZE });
    try {
      const response = await reviewApi.getReviews({
        page: pageIndex,
        size: PAGE_SIZE,
        ...filters,
      });
      if (requestId === reviewsRequest.current) {
        const data = response.data || {};
        setReviewPage({
          content: Array.isArray(data.content) ? data.content : Array.isArray(data) ? data : [],
          totalElements: Number(data.totalElements ?? data.length ?? 0),
          totalPages: Number(data.totalPages ?? 0),
          number: Number(data.number ?? pageIndex),
          size: Number(data.size ?? PAGE_SIZE),
        });
      }
    } catch (requestError) {
      if (requestId === reviewsRequest.current) setReviewsError(errorMessage(requestError));
    } finally {
      if (requestId === reviewsRequest.current) setReviewsLoading(false);
    }
  }, [appliedReviewFilters, page]);

  useEffect(() => { loadDashboard(dashboardFilters); }, [dashboardFilters, loadDashboard]);
  useEffect(() => { loadReviews(page, appliedReviewFilters); }, [page, appliedReviewFilters, loadReviews]);

  const refreshData = useCallback(async () => {
    await Promise.all([
      loadDashboard(dashboardFilters),
      loadReviews(0, appliedReviewFilters),
    ]);
    setPage(0);
  }, [loadDashboard, loadReviews, dashboardFilters, appliedReviewFilters]);

  const applyReviewFilters = () => {
    setPage(0);
    setAppliedReviewFilters({ ...reviewFilters });
  };

  const clearReviewFilters = () => {
    setReviewFilters(EMPTY_FILTERS);
    setAppliedReviewFilters(EMPTY_FILTERS);
    setPage(0);
  };

  const openReview = async (reviewId) => {
    const requestId = ++detailsRequest.current;
    setSelectedReview({ loading: true, data: null, error: '' });
    try {
      const response = await reviewApi.getReview(reviewId);
      if (requestId === detailsRequest.current) setSelectedReview({ loading: false, data: response.data, error: '', replyError: '' });
    } catch (requestError) {
      if (requestId === detailsRequest.current) setSelectedReview({ loading: false, data: null, error: errorMessage(requestError) });
    }
  };

  const generateReplyFromRow = async (reviewId) => {
    const requestId = ++detailsRequest.current;
    setSelectedReview({ loading: true, data: null, error: '', replyError: '' });
    try {
      const detailResponse = await reviewApi.getReview(reviewId);
      const detail = detailResponse.data;
      if (requestId !== detailsRequest.current) return;
      setSelectedReview({ loading: false, data: detail, error: '', replyError: '' });
      if (!detail.analysis?.needs_reply) {
        setSelectedReview((current) => current?.data?.reviewId === reviewId
          ? { ...current, replyError: 'This review is no longer marked as needing a reply.' }
          : current);
        return;
      }
      const replyResponse = await reviewApi.suggestReply(reviewId);
      if (requestId === detailsRequest.current) {
        setSelectedReview((current) => current?.data?.reviewId === reviewId
          ? { ...current, data: { ...current.data, suggestedReply: replyResponse.data.suggestedReply } }
          : current);
      }
    } catch (requestError) {
      if (requestId === detailsRequest.current) {
        setSelectedReview((current) => current?.data?.reviewId === reviewId
          ? { ...current, replyError: errorMessage(requestError) }
          : { loading: false, data: null, error: errorMessage(requestError), replyError: '' });
      }
    }
  };

  const closeReview = useCallback(() => {
    detailsRequest.current++;
    setSelectedReview(null);
  }, []);

  const generateReply = async (reviewId) => {
    const response = await reviewApi.suggestReply(reviewId);
    setSelectedReview((current) => current?.data?.reviewId === reviewId
      ? { ...current, data: { ...current.data, suggestedReply: response.data.suggestedReply } }
      : current);
    return response.data;
  };

  const navigate = (target) => {
    setActiveNav(target === 'upload' ? 'overview' : target);
    setSidebarOpen(false);
    if (target === 'overview') window.scrollTo({ top: 0, behavior: 'smooth' });
    else document.getElementById(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const analyzed = dashboard?.totalAnalyzedReviews ?? dashboard?.analyzedReviews;
  const counts = useMemo(() => dashboard?.sentimentCounts || {
    positive: dashboard?.positive,
    neutral: dashboard?.neutral,
    negative: dashboard?.negative,
  }, [dashboard]);
  const percentages = dashboard?.sentimentPercentages || {};
  const metricValue = (value) => value == null ? '—' : Number(value).toLocaleString();
  const metricPercent = (key) => percentages[key] == null ? 'Percentage unavailable' : `${percentages[key]}% of analyzed reviews`;
  const currentDate = new Intl.DateTimeFormat(undefined, { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date());

  return (
    <div className="app-shell">
      <div className={`sidebar-backdrop ${sidebarOpen ? 'visible' : ''}`} onClick={() => setSidebarOpen(false)} />
      <div className={`sidebar-shell ${sidebarOpen ? 'open' : ''}`}><Sidebar active={activeNav} onNavigate={navigate} /></div>
      <main className="main-content">
        <header className="topbar">
          <button className="mobile-menu icon-button" onClick={() => setSidebarOpen(true)} aria-label="Open navigation"><Menu size={20} /></button>
          <div className="breadcrumb"><span>Workspace</span><span className="breadcrumb-separator">/</span><strong>Overview</strong></div>
          <div className="topbar-right"><span className="date-label">{currentDate}</span><span className="topbar-avatar" aria-label="ReviewLens workspace">R</span></div>
        </header>

        <div className="content-wrap">
          <section className="welcome-row" id="overview">
            <div className="welcome-copy"><div className="section-kicker"><Sparkles size={13} /> REVIEW INTELLIGENCE</div>
              <h1>See the story behind<br className="desktop-break" /> every <span>review.</span></h1>
              <p>Understand what customers value, spot what needs attention, and respond with confidence.</p>
            </div>
            <div className="welcome-decoration" aria-hidden="true"><div className="deco-orbit orbit-one" /><div className="deco-orbit orbit-two" /><div className="deco-center"><Sparkles size={23} /></div><span className="deco-dot dot-a" /><span className="deco-dot dot-b" /><span className="deco-dot dot-c" /></div>
          </section>

          <DashboardFilters value={dashboardFilters} onApply={setDashboardFilters} />

          {dashboardError && <div className="api-alert" role="alert"><AlertCircle size={17} /><div><strong>Dashboard data is unavailable</strong><span>{dashboardError}</span></div><button className="icon-button" onClick={() => loadDashboard()} aria-label="Retry dashboard"><RotateCw size={15} /></button></div>}

          <section className="metrics-grid" aria-label="Review metrics">
            <MetricCard label="Analyzed reviews" value={metricValue(analyzed)} detail={dashboardLoading && !dashboard ? 'Loading insights' : `${metricValue(dashboard?.totalReviews)} total reviews`} icon={MessageSquareText} tone="total" loading={dashboardLoading && !dashboard} />
            <MetricCard label="Positive" value={metricValue(counts?.positive)} detail={metricPercent('positive')} icon={Smile} tone="positive" loading={dashboardLoading && !dashboard} />
            <MetricCard label="Neutral" value={metricValue(counts?.neutral)} detail={metricPercent('neutral')} icon={Meh} tone="neutral" loading={dashboardLoading && !dashboard} />
            <MetricCard label="Negative" value={metricValue(counts?.negative)} detail={metricPercent('negative')} icon={Frown} tone="negative" loading={dashboardLoading && !dashboard} />
            <MetricCard label="Needs a reply" value={metricValue(dashboard?.reviewsNeedingReply ?? dashboard?.needsReply)} detail="Customer follow-up" icon={MessageSquareText} tone="reply" loading={dashboardLoading && !dashboard} />
          </section>

          <section className="insights-grid" aria-label="Review insights">
            <SentimentPanel counts={counts} percentages={percentages} loading={dashboardLoading && !dashboard} unavailable={!dashboard && Boolean(dashboardError)} />
            <TopicLists complaints={dashboard?.topComplaints || []} praises={dashboard?.topPraises || []} unavailable={!dashboard && Boolean(dashboardError)} loading={dashboardLoading && !dashboard} />
          </section>

          {dashboard && Number(dashboard.totalReviews) === 0 && !dashboardLoading && <div className="data-empty-note"><span className="empty-dot" />No reviews match the selected dashboard filters. Adjust the filters or upload a CSV.</div>}

          <UploadPanel onUploaded={refreshData} />

          <ReviewsSection reviews={reviewPage.content} total={reviewPage.totalElements} page={page}
            totalPages={reviewPage.totalPages} pageSize={PAGE_SIZE} filters={reviewFilters}
            onFilterChange={(key, value) => setReviewFilters((current) => ({ ...current, [key]: value }))}
            onApplyFilters={applyReviewFilters} onClearFilters={clearReviewFilters}
            onPageChange={(nextPage) => { setPage(nextPage); window.scrollTo({ top: document.getElementById('reviews')?.offsetTop || 0, behavior: 'smooth' }); }}
            onViewReview={openReview} onSuggestReply={generateReplyFromRow} loading={reviewsLoading} error={reviewsError} />

          <footer className="app-footer"><span>ReviewLens</span><span>Customer feedback, brought into focus.</span><span className="footer-credit">Built for better listening <ArrowUpRight size={12} /></span></footer>
        </div>
      </main>

      {selectedReview && <ReviewDetailsDialog review={selectedReview.data} loading={selectedReview.loading} error={selectedReview.error} replyError={selectedReview.replyError} onClose={closeReview} onGenerateReply={generateReply} />}
    </div>
  );
}
