export const TOPICS = ['quality', 'delivery', 'price', 'packaging', 'support', 'size_fit', 'other'];

export default function FilterSelect({ id, label, value, onChange, options, allLabel = 'All' }) {
  return (
    <label className="filter-control" htmlFor={id}>
      <span>{label}</span>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">{allLabel}</option>
        {options.map((option) => (
          <option value={option.value ?? option} key={option.value ?? option}>
            {option.label ?? option.replaceAll('_', ' ')}
          </option>
        ))}
      </select>
    </label>
  );
}
