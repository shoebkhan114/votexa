import {
  Chart as ChartJS, CategoryScale, LinearScale, BarElement, ArcElement, LineElement, PointElement, Tooltip, Legend,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, LineElement, PointElement, Tooltip, Legend);

export const PALETTE = ['#4f46e5', '#16a34a', '#f59e0b', '#dc2626', '#0ea5e9', '#a855f7', '#64748b', '#14b8a6'];
