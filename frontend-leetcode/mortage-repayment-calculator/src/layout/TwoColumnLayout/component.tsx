import * as React from 'react';

export type TwoColumnLayoutProps = {
    leftColumn: React.ReactNode;
    rightColumn: React.ReactNode;
    leftColumnClassName?: string;
    rightColumnClassName?: string;
}

export const TwoColumnLayout = ({
    leftColumn,
    rightColumn,
    leftColumnClassName = "w-1/2 p-4 bg-gray-50",
    rightColumnClassName = "w-1/2 p-4 bg-blue-800 text-white"
}: TwoColumnLayoutProps) => {

    return (
        <div className="flex">
            <div className={leftColumnClassName}>
                {leftColumn}
            </div>
            <div className={rightColumnClassName}>
                {rightColumn}
            </div>
        </div>
    )
}