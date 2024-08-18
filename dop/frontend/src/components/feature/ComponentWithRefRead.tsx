import * as React from 'react';

function ComponentWithRefRead() {
    const [text, setText] = React.useState('Some text ...');

    function handleOnChange(event: React.ChangeEvent<HTMLInputElement>) {
        setText(event.target.value);
    }

    const ref = React.useRef<HTMLSpanElement>(null);

    React.useEffect(() => {
        console.log('re-rendering ...');

        if (ref.current) {
            const { width } = ref.current.getBoundingClientRect();
            console.log('width:', width)
            document.title = `Width:${width}`;
        } else {
            console.log('oops, ref.current is null');
        }

    }, []);

    return (
        <div>
            <input type="text" value={text} onChange={handleOnChange} />
            <div>
                <span ref={ref}>{text}</span>
            </div>
        </div>
    );
}

export { ComponentWithRefRead }; 