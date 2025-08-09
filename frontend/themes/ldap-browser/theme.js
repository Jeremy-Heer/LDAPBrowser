import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

// Import the Apache Directory Studio-inspired CSS
import '@vaadin/vaadin-lumo-styles/color.js';
import '@vaadin/vaadin-lumo-styles/typography.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import './styles.css';

// Additional styling for Directory Studio look
const directoryStudioTheme = `
  /* Eclipse-style look */
  html {
    --lumo-base-color: #f8f8f8;
    --lumo-primary-color: #4a90e2;
    --lumo-primary-text-color: #ffffff;
    --lumo-tint-5pct: rgba(74, 144, 226, 0.05);
    --lumo-tint-10pct: rgba(74, 144, 226, 0.1);
    --lumo-shade-5pct: rgba(0, 0, 0, 0.05);
    --lumo-shade-10pct: rgba(0, 0, 0, 0.1);
    --lumo-shade-20pct: rgba(0, 0, 0, 0.2);
  }
  
  /* Directory Studio style panels */
  .ds-panel {
    background: #ffffff;
    border: 1px solid #c0c0c0;
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.8);
  }
  
  .ds-panel-header {
    background: linear-gradient(to bottom, #f0f0f0 0%, #e0e0e0 100%);
    border-bottom: 1px solid #c0c0c0;
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.5);
    font-weight: 600;
    font-size: 0.9em;
    color: #333;
  }
  
  .ds-toolbar {
    background: linear-gradient(to bottom, #f8f8f8 0%, #e8e8e8 100%);
    border-bottom: 1px solid #c0c0c0;
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.8);
  }
  
  /* Professional button styling */
  .button-group vaadin-button {
    --lumo-button-size: var(--lumo-size-s);
    font-size: 0.85em;
  }
  
  /* Tree grid styling */
  .ldap-tree vaadin-grid {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 0.85em;
  }
  
  /* Status badges */
  .status-connected {
    background-color: #4CAF50;
    color: white;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 0.8em;
    font-weight: bold;
  }
  
  .status-disconnected {
    background-color: #f44336;
    color: white;  
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 0.8em;
    font-weight: bold;
  }
`;

// Inject the custom CSS
injectGlobalCss(directoryStudioTheme);
