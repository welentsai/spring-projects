import * as React from 'react';

type InputProps = {
    id: string;
    isFocused: boolean;
    value: string;
    type?: string;
    children: React.ReactNode;
    onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

const InputWithLabel = ({
    id,
    isFocused,
    value,
    type = 'text',
    children,
    onInputChange }: InputProps) => {

    const inputRef = React.useRef<HTMLInputElement>(null);

    React.useEffect(() => {
        if (isFocused && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isFocused]);

    return (
        <>
            <label htmlFor={id}>{children}</label>
            &nbsp;
            <input
                ref={inputRef}
                id={id}
                type={type}
                value={value}
                onChange={onInputChange}
            />
        </>
    )
}

export { InputWithLabel };