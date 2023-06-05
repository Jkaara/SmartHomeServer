import React from 'react';
import './index.css';
import App from './Components/App/App';
import reportWebVitals from './reportWebVitals';
import { CssBaseline, ThemeProvider, StyledEngineProvider } from "@mui/material";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { EvChargingStations } from "./Components/EvChargingStations/EvChargingStations";
import theme from "./theme";
import { EnergyStorageSystem } from "./Components/EnergyStorageSystem/EnergyStorageSystem";
import { createRoot } from 'react-dom/client';
import { UserSettingsAdmin } from "./Components/UserSettingsAdmin/UserSettingsAdmin";


const container = document.getElementById('root');

const root = createRoot(container!);

root.render(
    <React.StrictMode>
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <BrowserRouter basename={process.env.REACT_APP_BASENAME}>
                <Routes>
                    <Route path="/" element={<App/>}/>
                    <Route path="/evChargingStations" element={<EvChargingStations/>}/>
                    <Route path="/users" element={<UserSettingsAdmin/>}/>
                    <Route path="/energy" element={<EnergyStorageSystem/>}/>
                </Routes>
            </BrowserRouter>
        </ThemeProvider>
    </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
