import { BarChart3, ChevronDown, CircleHelp, FileText, Layers3, Sparkles } from 'lucide-react';

export default function Sidebar({ active, onNavigate }) {
  return (
    <aside className="sidebar">
      <a className="brand" href="#overview" onClick={() => onNavigate('overview')} aria-label="ReviewLens home">
        <span className="brand-mark"><Layers3 size={19} strokeWidth={2.2} /></span>
        <span className="brand-name">Review<span>Lens</span></span>
      </a>

      <div className="workspace-switcher">
        <span className="workspace-avatar">RL</span>
        <span className="workspace-label"><strong>ReviewLens</strong><small>Workspace</small></span>
        <ChevronDown size={15} className="muted-icon" />
      </div>

      <div className="nav-caption">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        <button className={`nav-item ${active === 'overview' ? 'active' : ''}`} onClick={() => onNavigate('overview')}>
          <BarChart3 size={17} /> <span>Overview</span>
        </button>
        <button className={`nav-item ${active === 'reviews' ? 'active' : ''}`} onClick={() => onNavigate('reviews')}>
          <FileText size={17} /> <span>Reviews</span>
        </button>
      </nav>

      <div className="sidebar-bottom">
        <div className="sidebar-tip">
          <span className="tip-icon"><Sparkles size={15} /></span>
          <strong>Turn feedback into action</strong>
          <p>Upload a CSV to uncover what customers love and where to improve.</p>
          <button onClick={() => onNavigate('upload')}>Upload reviews <span aria-hidden="true">↗</span></button>
        </div>
        <button className="help-link" type="button" onClick={() => window.open('https://github.com', '_blank', 'noopener,noreferrer')}>
          <CircleHelp size={16} /> Help center
        </button>
        <div className="profile-row">
          <div className="profile-avatar">S</div>
          <div className="profile-name"><strong>Workspace</strong><small>Local environment</small></div>
          <ChevronDown size={15} className="muted-icon" />
        </div>
      </div>
    </aside>
  );
}
