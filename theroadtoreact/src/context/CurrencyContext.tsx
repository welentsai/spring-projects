import React from "react";

interface CurrencyContextType {
    currency: string;
    setCurrency: (currency: string) => void;
}

const CURRENCIES = {
    Euro: {
        symbol: '€',
        label: 'Euro',
    },
    Usd: {
        symbol: '$',
        label: 'US Dollar',
    },
};

const CurrencyContext = React.createContext<CurrencyContextType | null>(null);

const useCurrency = () => {
    const context = React.useContext(CurrencyContext);

    if (context === null) {
        throw new Error('useCurrency must be used within a CurrencyProvider');
    }

    return context;
}

export { CurrencyContext, useCurrency, CURRENCIES }