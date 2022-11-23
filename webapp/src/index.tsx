import React from 'react';
import './index.css';
import App from './Components/App/App';
import reportWebVitals from './reportWebVitals';
import { CssBaseline, MuiThemeProvider } from "@material-ui/core";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import GarageDoor from "./Components/GarageDoor/GarageDoor";
import HeaterController from "./Components/Heater/HeaterController";
import { EvChargingStations } from "./Components/EvChargingStations/EvChargingStations";
import Webcams from "./Components/Webcams/Webcams";
import theme from "./theme";
import { EnvironmentSensors } from "./Components/EnvironmentSensors/EnvironmentSensors";
import { VideoRecordings } from "./Components/VideoRecordings/VideoRecordings";
import { EnergyPricingSettings } from "./Components/EnergyPricingSettings/EnergyPricingSettings";
import { EnergyStorageSystem } from "./Components/EnergyStorageSystem/EnergyStorageSystem";
import { createRoot } from 'react-dom/client';
const container = document.getElementById('root');

const root = createRoot(container!);

root.render(
    <React.StrictMode>
        <MuiThemeProvider theme={theme}>
            <CssBaseline/>
            <BrowserRouter basename={process.env.REACT_APP_BASENAME}>
                <Routes>
                    <Route path="/" element={<App/>}/>
                    <Route path="/garage" element={<GarageDoor/>}/>
                    <Route path="/heater" element={<HeaterController/>}/>
                    <Route path="/evChargingStations" element={<EvChargingStations/>}/>
                    <Route path="/environmentSensors" element={<EnvironmentSensors/>}/>
                    <Route path="/webcams" element={<Webcams/>}/>
                    <Route path="/recordings" element={<VideoRecordings/>}/>
                    <Route path="/energy_price_settings" element={<EnergyPricingSettings/>}/>
                    <Route path="/energy" element={<EnergyStorageSystem/>}/>
                </Routes>
            </BrowserRouter>
        </MuiThemeProvider>
    </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
