import { ref } from 'vue';

const stored = localStorage.getItem('uiTheme');
const systemDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches;
const theme = ref(stored === 'dark' || stored === 'light' ? stored : (systemDark ? 'dark' : 'light'));

function applyTheme(value) {
  document.documentElement.dataset.theme = value;
  document.documentElement.style.colorScheme = value;
}

applyTheme(theme.value);

export function useTheme() {
  function setTheme(value) {
    theme.value = value === 'dark' ? 'dark' : 'light';
    localStorage.setItem('uiTheme', theme.value);
    applyTheme(theme.value);
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark');
  }

  return { theme, setTheme, toggleTheme };
}
