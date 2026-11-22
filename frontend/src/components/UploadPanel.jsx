import { useRef, useState } from 'react';
import { ArrowUpRight, Check, FileSpreadsheet, FileUp, LoaderCircle, UploadCloud, X } from 'lucide-react';
import { errorMessage, reviewApi } from '../api/client.js';

export default function UploadPanel({ onUploaded }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const chooseFile = (candidate) => {
    if (!candidate) return;
    setResult(null);
    setError('');
    if (!candidate.name.toLowerCase().endsWith('.csv')) {
      setFile(null);
      setError('Choose a .csv file to upload.');
      return;
    }
    setFile(candidate);
  };

  const upload = async () => {
    if (!file || uploading) return;
    setUploading(true);
    setProgress(0);
    setError('');
    setResult(null);
    try {
      const response = await reviewApi.uploadCsv(file, (event) => {
        if (event.total) setProgress(Math.round((event.loaded / event.total) * 100));
      });
      setProgress(100);
      setResult(response.data);
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      await onUploaded?.();
    } catch (uploadError) {
      setError(errorMessage(uploadError));
    } finally {
      setUploading(false);
    }
  };

  const processing = result?.csvProcessing || {};
  const statusFailed = Array.isArray(processing.reviews)
    ? processing.reviews.filter((review) => String(review.analysisStatus).toLowerCase() === 'analysis_failed').length
    : 0;
  const resultFailed = Array.isArray(result?.analyses)
    ? result.analyses.filter((analysis) => analysis == null).length
    : 0;
  const failedCount = Math.max(statusFailed, resultFailed);

  return (
    <section className="upload-panel" id="upload">
      <div className="upload-panel-copy">
        <div className="eyebrow"><span className="eyebrow-dot" /> DATA INGESTION</div>
        <h2>Bring your reviews<br /><em>into focus.</em></h2>
        <p>Upload a CSV and ReviewLens will clean, analyze and organize your feedback.</p>
        <div className="upload-format"><FileSpreadsheet size={15} /><span>Required columns: review_id, product, rating, date, review_text</span></div>
      </div>
      <div className="upload-action-area">
        {result ? (
          <div className="upload-result" role="status">
            <div className="upload-result-heading"><span className="success-mark"><Check size={17} /></span><div><strong>Upload processed</strong><span>{Number(result.savedReviews) || 0} reviews saved</span></div>
              <button className="icon-button small" onClick={() => setResult(null)} aria-label="Dismiss upload results"><X size={15} /></button></div>
            <div className="upload-result-grid">
              <ResultValue label="Valid" value={processing.validReviews} tone="green" />
              <ResultValue label="Invalid" value={processing.invalidReviews} tone="rose" />
              <ResultValue label="Skipped" value={processing.skippedReviews} />
              <ResultValue label="Duplicates" value={processing.duplicateReviews} tone="amber" />
              <ResultValue label="Duplicate IDs" value={processing.duplicateReviewIds} tone="amber" />
              <ResultValue label="Analysis failed" value={failedCount} tone={failedCount ? 'rose' : 'green'} />
            </div>
            {Number(processing.totalRows) === 0 && <p className="upload-result-note">The backend returned no row details for this upload.</p>}
          </div>
        ) : (
          <div className={`dropzone ${dragging ? 'dragging' : ''} ${uploading ? 'is-uploading' : ''}`}
            onDragEnter={(event) => { event.preventDefault(); setDragging(true); }}
            onDragOver={(event) => event.preventDefault()}
            onDragLeave={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false); }}
            onDrop={(event) => { event.preventDefault(); setDragging(false); chooseFile(event.dataTransfer.files?.[0]); }}
            >
            <input ref={inputRef} className="visually-hidden" type="file" accept=".csv,text/csv" aria-label="Choose CSV file"
              onChange={(event) => chooseFile(event.target.files?.[0])} disabled={uploading} />
            <div className="drop-icon">{uploading ? <LoaderCircle className="spin" size={22} /> : <UploadCloud size={23} />}</div>
            {uploading ? <><strong>Analyzing your file…</strong><span className="drop-subtitle">{progress >= 100 ? 'File received · waiting for review analysis' : 'Uploading CSV to ReviewLens'}</span></>
              : file ? <><strong>{file.name}</strong><span className="drop-subtitle">{(file.size / 1024).toFixed(1)} KB · ready to process</span></>
                : <><strong>Drop your CSV here</strong><span className="drop-subtitle">or <button type="button" className="browse-link" onClick={() => inputRef.current?.click()}>browse files</button> from your computer</span></>}
            {uploading && <div className="progress-track" role="progressbar" aria-valuenow={progress} aria-valuemin="0" aria-valuemax="100"><span style={{ width: `${progress}%` }} /></div>}
            {!uploading && file && <div className="drop-controls">
              <button className="button button-primary button-small" onClick={(event) => { event.stopPropagation(); upload(); }}><FileUp size={15} /> Upload and analyze <ArrowUpRight size={14} /></button>
              <button className="icon-button small" aria-label="Remove selected file" onClick={(event) => { event.stopPropagation(); setFile(null); if (inputRef.current) inputRef.current.value = ''; }}><X size={15} /></button>
            </div>}
          </div>
        )}
        {error && <div className="inline-error" role="alert"><X size={15} />{error}</div>}
        <p className="privacy-note">Your review data stays in your ReviewLens workspace.</p>
      </div>
    </section>
  );
}

function ResultValue({ label, value, tone = '' }) {
  return <div className={`result-value ${tone}`}><span>{label}</span><strong>{Number(value) || 0}</strong></div>;
}
