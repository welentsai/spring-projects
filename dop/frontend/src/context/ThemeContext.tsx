import React from "react";

export type ThemeContextType = {
    theme: string;
    setTheme: (theme: string) => void;
}

const ThemeContext = React.createContext<ThemeContextType>({
    theme: "default",
    setTheme: () => { }
});

interface ThemeProviderProps {
    children: React.ReactNode;
}

const ThemeProvider = ({ children }: ThemeProviderProps) => {
    const [theme, setTheme] = React.useState("default");

    return (
        <ThemeContext.Provider value={{ theme, setTheme }}>
            {children}
        </ThemeContext.Provider>
    )
}

const useTheme = () => {
    const { theme, setTheme } = React.useContext(ThemeContext)

    const handleTheme = (value: string) => {
        setTheme(value);
    }

    return { value: theme, onChange: handleTheme }
}


export { ThemeProvider, useTheme };