import * as React from "react"

type CenterLayoutProps = {
    children: React.ReactNode;
}

export const CenterLayout = ({ children }: CenterLayoutProps) => {
    return (
        <div className="flex justify-center items-center min-h-screen bg-blue-50">
            {children}
        </div>
    )
}